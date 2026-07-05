# Fand API 开发文档

这份文档同时面向两类读者：

- 插件开发者：使用 `fand-api` 编写、注册和维护插件。
- API 贡献者：在 `fand-api` 中设计公共接口，并在 `fand-server` 中实现它们。

`fand-api` 是 Fand 的公共插件 API。插件代码只能依赖这里暴露的类型；不要依赖
`fand-server`、反编译后的 Minecraft 服务端源码、Bukkit、NMS 或 vanilla 内部类。

## 快速开始

推荐使用独立发布的 `io.fand.plugin` Gradle 插件构建插件工程；它会自动配置 Fand API 依赖、
校验或生成 `fand-plugin.json`，并给插件 jar 注入直接运行提示入口。

插件 jar 根目录需要包含 `fand-plugin.json`：

```json
{
  "id": "example-plugin",
  "version": "0.1.0",
  "mainClass": "com.example.ExamplePlugin",
  "description": "Example Fand plugin",
  "website": "https://example.com",
  "license": "MIT",
  "apiVersion": "0.1.1",
  "authors": ["Example"],
  "depends": [],
  "loadAfter": [],
  "loadBefore": [],
  "permissions": [
    {
      "node": "example-plugin.admin",
      "defaultAccess": "OPERATOR",
      "children": {
        "example-plugin.reload": true
      }
    }
  ]
}
```

`mainClass` 指向一个实现 `io.fand.api.plugin.Plugin` 的类。运行时会用无参构造器实例化
该类，并按顺序调用 `onLoad`、`onEnable`、`onDisable`。

`description`、`website`、`license` 是展示和审计元信息，不影响加载顺序；`website` 非空时必须使用
`http` 或 `https`。`apiVersion` 声明插件面向的 Fand API 版本，旧插件未声明时按当前 API 版本处理。
只包含 `plugin.yml`、`paper-plugin.yml`、`fabric.mod.json`、`META-INF/mods.toml` 等其它加载器描述文件的
jar 会被诊断为对应 Bukkit/Paper/Fabric/Forge 等插件，而不会作为 Fand 插件加载。

`depends` 是硬依赖：目标插件必须存在并先加载，依赖插件也能通过 classloader 访问被依赖插件。
`loadAfter` / `loadBefore` 是软加载顺序：只在目标插件同时存在时影响拓扑排序，目标缺失时忽略，
不提供 classloader 可见性，也不会参与卸载级联依赖。软排序与硬依赖共同形成加载顺序图；出现循环时，
严格模式报错，默认容错模式会跳过循环中的插件。

```java
package com.example;

import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginContext;
import java.time.Duration;
import net.kyori.adventure.text.Component;

public final class ExamplePlugin implements Plugin {

    @Override
    public void onLoad(PluginContext context) {
        context.logger().info("loaded {}", context.descriptor().id());
    }

    @Override
    public void onEnable(PluginContext context) {
        context.events().subscribe(PlayerJoinEvent.class, event ->
                event.player().sendMessage(Component.text("Welcome to Fand")));

        context.scheduler().runMainRepeating(
                () -> context.logger().debug("heartbeat"),
                Duration.ofSeconds(30),
                Duration.ofSeconds(60));
    }

    @Override
    public void onDisable(PluginContext context) {
        context.logger().info("disabled {}", context.descriptor().id());
    }
}
```

插件应当在 `onEnable` 中注册监听器、命令、任务和其他运行时资源。除非明确需要生命周期外的
静态元数据，不要保留 `onEnable` 之前拿到的运行时引用。

## 插件运行时入口

`PluginContext` 是插件的主入口。优先使用 `context` 上的服务，而不是全局 `Fand.server()`，
因为 `context` 上的注册通常带插件生命周期作用域，插件禁用时会自动清理。

常用入口：

- `context.logger()`：带插件 id 的 SLF4J logger。
- `context.events()`：插件作用域事件总线。
- `context.commands()`：插件作用域命令注册表。
- `context.scheduler()`：插件作用域调度器。
- `context.config()` / `context.reloadConfig()`：插件 `config.yml`。
- `context.storage()`：插件持久化数据。
- `context.permissions()`：权限注册和检查。
- `context.guis()`：轻量 GUI 路由服务。
- `context.packets()`：包拦截和自定义 payload 通道。
- `context.recipes()`：配方注册表。
- `context.scoreboard()`：vanilla 计分板服务。
- `context.services()`：跨插件 Java provider 注册表。
- `context.customItems()` / `context.customBlocks()`：插件命名空间下的自定义物品和方块。

`Fand.server()` 返回当前运行中的 `Server`。它适合读取全局状态、查找在线玩家、访问世界、
广播 Adventure 消息、创建或卸载动态世界、读取性能快照等。插件拥有的注册仍应优先走
`PluginContext`。

插件也可以在 `fand-plugin.json` 的 `permissions` 中声明权限树。`node` 和 `children` 必须属于插件
自己的命名空间；运行时会在插件加载阶段自动注册这些权限，`children` 在父权限实际授予时展开。

## 事件

事件通过 `EventBus` 同步派发，也可以用 `fireAsync` 在指定 executor 上异步派发。监听器运行在
触发事件的线程上；事件总线不会自动切换到主线程。需要修改世界、实体或其他主线程状态时，
监听器应通过 `context.scheduler().runMain(...)` 跳回主线程。

直接注册：

```java
context.events().subscribe(PlayerJoinEvent.class, event ->
        context.logger().info("{} joined", event.player().name()));
```

注解注册：

```java
import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
import io.fand.api.event.player.PlayerQuitEvent;

final class PlayerListener implements Listener {

    @Subscribe
    void onQuit(PlayerQuitEvent event) {
        event.player().sendMessage(Component.text("Goodbye"));
    }
}

context.events().registerListener(new PlayerListener());
```

可取消事件实现 `Cancellable`。监听器可以读取或修改事件对象；事件在所有监听器执行完成后回到
触发点。

## 命令

命令可以通过链式 Builder 或注解类注册。命令通常在 `onEnable` 中注册到
`context.commands()`，插件禁用时由运行时自动清理。

```java
import net.kyori.adventure.text.Component;

context.commands().register("hello", command -> command
        .aliases("hi")
        .permission("example.hello")
        .executes(context -> context.sender().sendMessage(Component.text("Hello, " + context.sender().name())))
        .argument("target", io.fand.api.command.Arguments.greedyString(), target -> target
                .executes(context -> context.sender().sendMessage(Component.text("Hello, " + context.string("target"))))));
```

独立命令类可以用注解注册：

```java
import io.fand.api.command.Arg;
import io.fand.api.command.Command;
import io.fand.api.command.CommandContext;
import io.fand.api.command.Default;
import io.fand.api.command.Permission;
import io.fand.api.command.Subcommand;

@Command("hello")
@Permission("example.hello")
public final class HelloCommand {
    @Default
    public void self(CommandContext context) {
        context.sender().sendMessage(Component.text("Hello, " + context.sender().name()));
    }

    @Subcommand("to")
    public void target(CommandContext context, @Arg("name") String name) {
        context.sender().sendMessage(Component.text("Hello, " + name));
    }
}

context.commands().register(new HelloCommand());
```

命令权限节点应配合 `PermissionService` 注册默认访问策略。

## 配置、存储和权限

`context.config()` 对应插件数据目录下的 `config.yml`。第一次访问时，运行时会优先从插件 jar
根目录复制默认 `config.yml`；如果 jar 内没有默认配置，则创建空文档。`Configuration` 的读写
不是线程安全的；多线程访问时需要调用方自行同步。

```java
var enabled = context.config().getBoolean("features.enabled", true);
context.config().set("features.enabled", enabled);
context.config().save();
```

`context.storage()` 用于插件持久化玩法数据，按作用域拆分：

- `global()`：插件全局数据。
- `player(UUID)`：玩家数据。
- `world(Key)`：世界数据。
- `chunk(Key, int, int)`：区块数据。
- `block(Key, int, int, int)` / `block(Block)`：方块数据。

权限通过 `PermissionDescriptor` 声明：

```java
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;

context.permissions().register(new PermissionDescriptor("example.hello", PermissionDefault.TRUE));
```

`PermissionDefault` 支持 `TRUE`、`FALSE`、`OPERATOR`、`NOT_OPERATOR`。

## 调度和线程

`Scheduler` 提供主线程任务和异步任务：

- `runMain`：安排到下一个 tick。
- `runMainAfter` / `runMainAfterTicks`：延迟执行。
- `runMainRepeating` / `runMainRepeatingTicks`：重复主线程任务。
- `runAsync` / `runAsyncAfter`：后台任务。

主线程任务遵守 tick 边界。按 `Duration` 延迟的方法至少等待对应时间；按 tick 延迟的方法按已完成
服务器 tick 计数，不依赖墙钟速度。异步任务和主线程之间没有顺序保证；异步代码不要直接修改世界、
实体、库存或其他主线程状态。

## 玩法 API

`Server`、`World`、`Player`、`Entity`、`Inventory`、`ItemStack`、配方、计分板、GUI、包和性能 API
组成主要玩法表面：

- `Server` 是 Adventure `ForwardingAudience`，`server().sendMessage(...)` 会广播给所有在线玩家。
- `Server.players()`、`Server.worlds()` 等集合返回快照。
- 世界、实体、方块、物品类型通过 Adventure `Key` 或生成的 vanilla key 查询。
- `RecipeRegistry.register` 会更新实时 vanilla 配方管理器，返回的注册句柄关闭后移除同一个配方。
- `ScoreboardService` 操作持久 vanilla 计分板目标和队伍。
- `ServiceRegistry.providers(type)` 按 `ServicePriority` 高到低返回；同优先级后注册者优先。
  `provider(type)` 取排序后的第一个，当前 provider 注销后自然 fallback 到下一个仍 active 的 provider。
- `RegionService.applicableRegions(location)` 按 protection priority 高到低、同优先级体积小优先、
  再同则后注册优先。`resolveFlag` 按这个顺序解析；每个 region 先查自身显式 flag，再查 parent 链。
  parent 命中后不会继续查低优先级重叠 region。
- `LivingEntity` 暴露空气、冻结、无敌 tick、视线判断和睡眠/唤醒控制；玩家睡眠走 vanilla 床规则。
- `FurnaceBlockEntity`、`BrewingStandBlockEntity`、`BeehiveBlockEntity`、`SculkSensorBlockEntity`
  暴露常用运行状态，插件可以读写计时、燃料、蜂巢释放和振动频率。
- `GuiService.open(Player, Gui)` 打开轻量 GUI，并把库存事件路由到对应 `GuiView`。
- `PacketRegistry` 支持 vanilla 包拦截和自定义 payload 通道；优先使用 `context.packets()`。
  `builder(type)` 用字段 map 创建 `PacketView`，`helpers()` 提供 entity metadata、display/hologram entity、
  scoreboard team/nameplate 和 open screen 等常见 clientbound 包的字段预填入口。
- `PlaceholderService` 仍支持 `viewer + identifier` 的旧入口；需要 PAPI 风格关系上下文时，使用
  `PlaceholderContext` 携带 viewer、target、world、entity 和任意上下文 map。
- `Server.performance()` 返回最新 tick 性能快照，适合命令和监控展示。

## API 贡献规则

新增或修改公共 API 时，必须把 `fand-api` 当成稳定边界：

- 不从 `fand-api` 暴露 Bukkit、NMS、vanilla 内部类或 `fand-server` 实现类型。
- 包默认使用 `@NullMarked`；可空引用显式标注 `@Nullable`。
- 数据载体优先使用 `record`；封闭类型集合优先使用 `sealed` 层级；返回集合优先不可变或快照。
- 生命周期绑定的注册必须有插件作用域入口，并且插件禁用时能自动清理。
- 任何非主线程运行或可能跨线程完成的公共方法，都要在 JavaDoc 中写清线程模型。
- API 命名应表达 Fand 自己的领域模型，不照搬 Bukkit 名称，除非语义确实一致。
- 不为了单个实现细节增加公共标志、宽泛扩展点或 “just in case” 方法。

实现通常位于 `fand-server`，但插件看到的类型仍然只能来自 `fand-api`。实现可以适配 vanilla 状态，
公共接口不能泄漏这些适配细节。

## 生成表面和包 API

大量 vanilla registry key、packet metadata 和相似 API 表面应由 `fand-data-generator` 生成或校验，
不要手写成百上千个重复映射。生成逻辑必须稳定排序、稳定格式化，并能从当前 Minecraft 源码复现
同一 API 形状。

包 API 有额外约束：

- `PacketType`、typed packet views 和服务端 vanilla 映射必须来自同一份 metadata。
- 只有能通过 `PacketType#viewType()` 到达的 view 才是有效 API。
- 包视图只暴露 API 安全值；不能泄漏 NMS、Bukkit 或 vanilla 类。
- 只有实现能精确重建 vanilla 包时才允许替换包。
- 不使用通用反射让未知 class packet 变成可替换；字段顺序和构造器顺序不是 API 合约。
- 自定义 payload 的方向是合约：入站定义需要 handler，出站定义不能注册入站 handler，双向定义需要同时满足两边。
- 关闭旧注册句柄只能移除它自己安装的资源，不能移除同 key 的新注册。

## 测试要求

公共 API 变更需要覆盖 `fand-api` 单元测试和对应 `fand-server` 实现测试。按风险选择测试层级：

- 纯 API 数据结构、枚举、值对象：放在 `fand-api/src/test/`。
- 运行时实现、注册清理、权限、命令、事件、配置、GUI、包桥接：放在 `fand-server/src/test/`。
- 需要真实 patched server 启停、插件加载、关服验证：放在 `fand-server/src/integrationTest/`。
- 生成 API 变更至少运行 `:fand-api:generateFandData` 和受影响 API 测试。

文档-only 变更不需要运行 Gradle 测试，但提交前应检查 Markdown 标题层级、代码块语言、相对路径和
示例是否仍对应真实 API。
