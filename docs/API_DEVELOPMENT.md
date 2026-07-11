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

## 外部库

插件可以在 jar 根目录放置可选的 `libraries.json`，让 Fand 在加载插件前下载未打包进插件 jar 的运行时库：

```json
{
  "repositories": [
    "https://repo.maven.apache.org/maven2/"
  ],
  "libraries": [
    "org.xerial:sqlite-jdbc:3.49.1.0",
    "org.postgresql:postgresql:42.7.5"
  ]
}
```

`repositories` 按声明顺序回退，只接受不含凭据、查询参数和 fragment 的绝对 `http` 或 `https` URL。
`libraries` 中的根依赖必须使用固定的 `group:artifact:version`，不接受动态版本。Fand 使用 Maven Resolver 读取
POM，并按 runtime classpath 语义解析传递依赖、scope、optional、exclusion 和同一依赖图中的版本冲突。
插件 jar 始终位于解析出的库之前；Fand API、Adventure、SLF4J 等平台类型仍由父 classloader 提供。

下载结果按 Maven 目录结构缓存在服务端根目录的 `libraries/`，相同坐标只保存一份。两个插件使用同一版本时会
复用缓存文件；声明不同版本时，两个版本会同时保留并由各自插件 classloader 隔离。Fand 不会把不同版本强制合并，
因为全局选择“最高版本”可能让原本兼容的插件在运行时产生链接错误。

每个仓库请求会对瞬时网络错误以及 HTTP 429、503 最多重试 3 次，再按声明顺序尝试后续仓库。远程仓库必须提供
可通过的 Maven 校验和；下载后 Fand 还会计算并持久化 `.sha256`，后续加载发现缓存内容变化时拒绝使用。

需要可复现供应链锁定时，可以增加 `sha256` 对象。键使用 Maven artifact 坐标，值为 64 位十六进制 SHA-256：

```json
{
  "repositories": ["https://repo.example.com/releases/"],
  "libraries": ["com.example:standalone:1.0.0"],
  "sha256": {
    "com.example:standalone:1.0.0": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
  }
}
```

一旦提供 `sha256`，它必须完整覆盖 Maven 解析出的每个 jar，且不能包含未解析的条目。这同时锁定依赖图和文件内容；
任一传递依赖缺少哈希、出现额外依赖或内容不匹配时，对应插件加载失败。非标准 artifact 可以使用
`group:artifact:extension:version` 或 `group:artifact:extension:classifier:version` 作为键。

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
- `context.resourcePacks()`：插件作用域资源包文件树和构建服务。
- `context.localization()`：插件 `messages/*.yml` 消息目录和本地化渲染。
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

命令使用结构化命令树注册。推荐在 `onEnable` 中注册到 `context.commands()`；通过
`PluginContext` 注册的命令会自动归属到当前插件命名空间，并在插件禁用时由运行时自动清理。

Fand 0.6 起不再推荐旧的 `CommandDescriptor + executor + completer` 写法。新命令 API 有两种入口：

- Builder：适合在插件主类或服务类中就地声明简单命令。
- 注解类：适合把较大的命令拆到独立类中维护。

### Builder 命令

Builder 以根命令名开始，继续声明别名、权限、默认执行器、字面量子命令和类型化参数：

```java
import io.fand.api.command.Arguments;
import net.kyori.adventure.text.Component;

context.commands().register("hello", root -> root
        .aliases("hi")
        .permission("example.hello")
        .executes(context -> context.sender().sendMessage(Component.text("Hello, " + context.sender().name())))
        .literal("to", to -> to
                .argument("target", Arguments.greedyString(), target -> target
                        .executes(context -> context.sender().sendMessage(Component.text("Hello, " + context.string("target")))))));
```

上面的命令提供：

- `/hello`：向执行者问候。
- `/hello to <target>`：读取 `target` 参数并问候目标。
- `/hi`：根命令别名。

插件作用域的 `context.commands()` 会覆盖命令定义中的 namespace，保证命令属于当前插件。普通插件通常
不需要调用 `.namespace(...)`；服务端内置命令或测试中直接使用全局 `CommandRegistry` 时才需要手动指定。

子命令用 `.literal(...)` 表示固定词，用 `.argument(...)` 表示参数节点。每个可执行节点都要设置
`.executes(...)`；权限可以放在根节点，也可以放在某个子命令节点上：

```java
context.commands().register("config", root -> root
        .permission("example.config")
        .executes(context -> showHelp(context.sender()))
        .literal("reload", reload -> reload
                .permission("example.config.reload")
                .executes(context -> reloadConfig(context.sender())))
        .literal("set", set -> set
                .argument("key", Arguments.word(), key -> key
                        .argument("value", Arguments.greedyString(), value -> value
                                .executes(context -> setConfig(
                                        context.sender(),
                                        context.string("key"),
                                        context.string("value")))))));
```

命令树会保留声明顺序，参数节点后可以继续接字面量节点：

```java
context.commands().register("confirm", root -> root
        .argument("target", Arguments.word(), target -> target
                .literal("yes", yes -> yes
                        .executes(context -> confirm(context.string("target"))))));
```

常用参数工厂在 `Arguments` 中：

- `word()`：单个无空格词。
- `string()`：一个 Brigadier 字符串参数。
- `greedyString()`：吞掉剩余输入，适合消息、理由和配置值。
- `bool()`：`true` 或 `false`。
- `integer()` / `integer(min, max)`：整数，可限制范围。
- `longValue(min, max)`、`floatValue()`、`doubleValue()`：数字参数。
- `player()`：在线玩家。
- `item()`：物品注册表 key。
- `enumValue(...)`：有限字符串集合。

参数可以声明为可选、带默认值，或在发送者类型匹配时默认使用发送者：

```java
context.commands().register("giveitem", root -> root
        .argument("item", Arguments.item(), item -> item
                .argument("amount", Arguments.integer(1, 2304).asOptional(1), amount -> amount
                        .executes(context -> giveItem(
                                context.sender(),
                                context.item("item"),
                                context.intValue("amount"))))));
```

补全可以直接挂在参数定义上，也可以挂在当前命令节点上。参数静态补全适合枚举值；节点补全适合需要根据
已解析参数动态计算的场景。运行时会按玩家正在输入的前缀过滤补全结果，补全回调只需要返回候选列表：

```java
context.commands().register("mode", root -> root
        .argument("value", Arguments.enumValue("fast", "safe", "debug"), value -> value
                .executes(context -> setMode(context.string("value")))));

context.commands().register("warp", root -> root
        .argument("name", Arguments.word(), name -> name
                .suggests(context -> knownWarpsFor(context.sender()))
                .executes(context -> warp(context.sender(), context.string("name")))));
```

`CommandContext` 提供本次调用的结构化数据：

- `sender()` / `sender(SomeSender.class)`：命令发送者。
- `label()`：玩家实际使用的根命令名，可能是别名。
- `args()`：原始参数列表。
- `arguments()`：已解析参数 map。
- `contains(name)`、`argument(name, type)`、`optionalArgument(name, type)`：通用访问。
- `string(name)`、`intValue(name)`、`booleanValue(name)`、`player(name)`、`item(name)` 等：常用类型访问。

`context.commands().register(...)` 会返回 `CommandRegistration`。如果命令只跟随插件生命周期，通常不需要保存；
运行时会在插件禁用时清理。需要临时命令时，可以保存句柄并调用 `unregister()` 或 `close()`。

需要先构造命令定义再注册时，可以使用 `CommandBuilder.command(...)` 或 `CommandBuilder.define(...)`：

```java
import io.fand.api.command.CommandBuilder;

var definition = CommandBuilder.command("tool")
        .literal("reload", reload -> reload.executes(context -> reloadTool(context.sender())))
        .build();

context.commands().register(definition);

context.commands().register(CommandBuilder.define("echo", root -> root
        .argument("message", Arguments.greedyString(), message -> message
                .executes(context -> context.sender().sendMessage(Component.text(context.string("message")))))));
```

### 注解命令

独立命令类可以用注解注册。类上使用 `@Command`、`@Aliases` 和 `@Permission` 声明根命令；方法上使用
`@Default` 或 `@Subcommand` 声明可执行节点：

```java
import io.fand.api.command.Arg;
import io.fand.api.command.Aliases;
import io.fand.api.command.Command;
import io.fand.api.command.CommandContext;
import io.fand.api.command.Default;
import io.fand.api.command.Permission;
import io.fand.api.command.Subcommand;
import net.kyori.adventure.text.Component;

@Command("hello")
@Aliases({"hi", "hey"})
@Permission("example.hello")
public final class HelloCommand {
    @Default
    public void self(CommandContext context) {
        context.sender().sendMessage(Component.text("Hello, " + context.sender().name()));
    }

    @Subcommand("to")
    public void target(CommandContext context, @Arg(value = "name", type = io.fand.api.command.CommandArgumentType.GREEDY_STRING) String name) {
        context.sender().sendMessage(Component.text("Hello, " + name));
    }
}

context.commands().register(new HelloCommand());
```

`@Subcommand` 可以包含多段路径，例如 `@Subcommand("config reload")`。方法参数可以是 `CommandContext`，
也可以是带 `@Arg` 的类型化参数。未显式指定 `type` 时，运行时会按 Java 参数类型推断：

- `String` 默认是 `WORD`，需要空格内容时指定 `type = CommandArgumentType.GREEDY_STRING`。
- `int` / `Integer`、`long` / `Long`、`boolean` / `Boolean`、`float` / `Float`、`double` / `Double`
  会映射到对应数字或布尔参数。
- `Player` 会映射为玩家参数。
- `ItemType` 会映射为物品注册表 key。

注解参数同样支持补全、范围和默认值：

```java
@Command("demo")
@Permission("example.demo")
public final class DemoCommand {
    @Subcommand("repeat")
    public void repeat(
            CommandContext context,
            @Arg(value = "times", min = 1, max = 10, optional = true, defaultInt = 1) int times,
            @Arg(value = "message", type = io.fand.api.command.CommandArgumentType.GREEDY_STRING) String message
    ) {
        for (var i = 0; i < times; i++) {
            context.sender().sendMessage(Component.text(message));
        }
    }

    @Subcommand("mode")
    public void mode(@Arg(value = "value", type = io.fand.api.command.CommandArgumentType.ENUM, suggestions = {"fast", "safe"}) String value) {
        setMode(value);
    }
}
```

命令权限节点应配合 `PermissionService` 或 `fand-plugin.json` 的 `permissions` 声明默认访问策略。

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

权限检查使用 `can(...)`，`allowed(...)` 是同语义别名。命令发送者和玩家可以直接检查单个权限；
需要完整权限解析、默认值、父子权限和附件覆盖时，优先通过 `PermissionService`：

```java
if (context.sender().can("example.admin")) {
    context.sender().sendMessage(Component.text("Access granted"));
}

var allowed = context.permissions().can(player, "example.hello");
```

`PermissionSubject.permissionValue(node)` 只表示主体自身显式声明的三态值，不会展开权限树、默认访问或附件；
插件通常不应直接用它做最终权限判断。

权限元数据由 `PermissionProvider` 提供，适合接入 LuckPerms 风格的 prefix、suffix、primary group 和 group
列表。插件通过 `context.services()` 注册 provider 后，运行时会自动接入 `context.permissions().meta(...)`、
`prefix(...)`、`group(...)` 和 `groups(...)`，插件禁用时也会自动清理：

```java
context.services().register(
        Key.key("example", "permissions"),
        PermissionProvider.class,
        myPermissionProvider,
        ServicePriority.HIGH);
```

多个 provider 按 `ServicePriority` 高到低查询，同优先级后注册者优先；当前 provider 注销后自动 fallback。

## 资源包和本地化

`context.resourcePacks()` 提供插件作用域资源包文件树。运行时会把资源包 id 自动限制到插件命名空间下，
插件禁用时会清理通过注册句柄创建的托管资源包。资源文件可以写入标准 vanilla 资源包路径，例如
`assets/<plugin-id>/textures/...`、`assets/<plugin-id>/models/...`、`assets/<plugin-id>/lang/...`。

```java
var pack = context.resourcePacks().create("main", "Example plugin resources");
context.resourcePacks().writeText(
        "main",
        "assets/example-plugin/lang/zh_cn.json",
        """
        {
          "item.example-plugin.magic_wand": "魔法法杖"
        }
        """);

var build = context.resourcePacks().build("main");
context.logger().info("Built resource pack {} sha1={} size={}", build.file(), build.sha1(), build.size());
```

`build(...)` 会在服务端 `resourcepacks/builds` 下生成 zip，并返回 SHA-1、文件路径和大小。发送给玩家时仍需要
一个客户端可访问的下载 URL：

```java
var request = context.resourcePacks().request("main", "https://cdn.example.com/example-plugin-main.zip", true, null);
player.sendResourcePack(request);
```

第一版资源包 API 不内置 HTTP 托管。推荐插件把 build 产物上传到自己的 CDN、静态文件服务，或等待后续专门的
资源包托管 API；核心服务端不在启动路径里额外打开下载服务。

插件资源包可以写 `assets/minecraft/...` 用于语言、字体等 vanilla namespace 覆盖；除此之外，`assets/...`
路径必须留在 `assets/<plugin-id>/...` 下。运行时会先规范化路径再检查命名空间，`../` 逃逸会被拒绝。

`context.localization()` 读取插件数据目录的 `messages/*.yml`，文件名使用 locale id，例如 `zh_cn.yml`、
`en_us.yml`。首次访问时，如果插件 jar 内带有 `messages/en_us.yml`，运行时会复制默认 fallback 文件到
数据目录。消息查找顺序为：精确 locale、语言 fallback、默认 locale、默认语言，最后返回 key 本身。

```yaml
# plugins/example-plugin/messages/zh_cn.yml
welcome: "<green>欢迎 {name}</green>"
errors:
  no_permission: "<red>你没有权限执行这个命令</red>"
```

```java
var message = context.localization().message(
        java.util.Locale.forLanguageTag("zh-CN"),
        "welcome",
        java.util.Map.of("name", player.name()));

var component = context.localization().component(
        player,
        "errors.no_permission");
player.sendMessage(component);
```

本地化文本会先应用 `{name}` 这种变量，再执行 Fand placeholder 替换，最后按 MiniMessage 渲染为
Adventure `Component`。如果只需要纯文本，使用 `message(...)`；需要颜色、点击事件或 placeholder 时使用
`component(...)`。

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
- `Server.tags()` 提供 vanilla data-pack 标签只读查询，覆盖 `BLOCK`、`ITEM`、`ENTITY_TYPE`、`FLUID` 和
  `DAMAGE_TYPE`。
- `Inventory` 提供槽位读写和批量工具：`contents()` 返回槽位快照，`setContents(...)` 从 0 号槽替换并清空剩余槽，
  `firstEmpty()` 查找空槽，`empty()` 判断是否全空，`count(...)` / `contains(...)` 支持按物品类型或完整
  `ItemStack` 查询，`remove(...)` 返回实际移除数量。没有查看者跟踪的库存 `viewers()` 返回空快照。
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
