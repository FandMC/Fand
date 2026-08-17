# Fand 自定义物品与自定义方块

本文档描述当前 Fand 源码中已经实现的自定义内容模型，并给出可以直接对应
`fand-api` 的示例。完整可运行参考位于
[`test-plugin`](../test-plugin/src/main/java/io/fand/testplugin/TestPlugin.java)。

## 运行模型

Fand 的自定义物品和方块拥有独立的逻辑 ID，但不要求玩家安装客户端模组：

- `CustomItemType` 由逻辑 ID、任意 vanilla `ItemType` base、默认物品组件和自定义方块工具规则组成。
- `CustomBlockType` 由逻辑 ID、任意 vanilla `BlockType` carrier、carrier state、持久组件、
  tick 设置、挖掘属性、自定义标签和 base 行为策略组成。
- 服务端 API、事件和库存往返时保留逻辑类型；发送给原版客户端时仍编码为选定的 vanilla base 加组件。
- 世界中实际放置的是 vanilla carrier。Fand 把逻辑方块 ID 和插件组件持久化在方块位置上。

这不是客户端模组注册表。客户端碰撞、方块形状、方块实体、光照基础行为和渲染入口仍来自
vanilla carrier；独立纹理和模型通过资源包提供。

插件应通过 `PluginContext.customItems()` 和 `PluginContext.customBlocks()` 注册。运行时会把逻辑 ID、
自定义标签和工具规则限制到当前插件命名空间，并在插件禁用时清理注册句柄。普通插件不要使用
`Fand.server().customItems()` 或 `Fand.server().customBlocks()` 创建插件自有注册。

下文示例假定 `fand-plugin.json` 的插件 id 是 `example`。如果实际插件 id 不同，应同步替换 Java key
和资源路径中的 `example`。插件作用域注册表会把逻辑物品 ID、逻辑方块 ID、自定义标签和自定义方块
工具规则改写到描述文件中的插件命名空间；显式 item model key 和资源路径不会被自动改名，越过插件
命名空间的 `assets/...` 路径会被拒绝。

## 最小自定义物品

不需要独立纹理时，可以复用 base 的 vanilla 模型。下面的物品仍有独立逻辑 ID 和名称，但客户端继续
显示原版木棍：

```java
import io.fand.api.item.ItemKey;
import io.fand.api.item.ItemTypes;
import io.fand.api.item.custom.CustomItemType;
import io.fand.api.plugin.PluginContext;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

private static final Key TOKEN_ID = Key.key("example:token");

private CustomItemType registerToken(PluginContext context) {
    var template = ItemTypes.of(ItemKey.STICK).one()
            .withItemName(Component.text("Example Token", NamedTextColor.GOLD))
            .withLore(Component.text("Logical item, vanilla model", NamedTextColor.GRAY));

    return context.customItems().register(CustomItemType.builder(TOKEN_ID, template.type())
            .components(template.components())
            .useBaseItemModel()
            .build()).type();
}
```

`CustomItemType` 默认把 `minecraft:item_model` 设置为逻辑 ID。`useBaseItemModel()` 明确把它改回 base
物品的 key，因此这个示例不需要资源包。Builder 调用顺序有意义：先放入模板组件，再选择最终模型。

创建、识别和修改栈：

```java
var stack = token.stack(16);
var one = token.one();
var sameType = context.customItems().customItem(stack).orElseThrow();
var id = context.customItems().customId(stack).orElseThrow();

var namedForPlayer = stack.withCustomName(Component.text("Alice's Token"));
player.inventory().add(namedForPlayer);
```

类型的 `defaultComponents` 会先应用，`ItemStack.componentPatch()` 保存单个栈的覆盖，
`ItemStack.components()` 返回两者合并后的有效组件。不要手动写入内部的 `fand:custom_item` 标记；
注册表会在编码和解码时维护它。

已有 vanilla 栈可以在 base 一致时转换为已注册逻辑类型，适合数据迁移或配方输出接入：

```java
var vanillaPaper = ItemTypes.of(ItemKey.PAPER).stack(4);
var rubyStack = context.customItems().tag(vanillaPaper, RUBY_ID);

// 只允许移除当前插件拥有的逻辑类型；其它插件的物品保持不变。
var untypedPaper = context.customItems().untag(rubyStack);
```

base 不一致时 `tag(...)` 会抛出 `IllegalArgumentException`，而不是悄悄替换物品类型。`untag(...)`
只移除逻辑身份和内部标记，会保留该栈当前有效的名称、模型及其它组件；需要恢复完全原版的栈时，
应明确创建新的 base `ItemStack` 或移除对应组件。

## 带独立纹理的物品

不调用 `useBaseItemModel()` 时，默认 item model 就是物品逻辑 ID。可以使用资源包模型生成器创建现代
item definition、模型和纹理：

```java
private static final String PACK_ID = "content";
private static final Key RUBY_ID = Key.key("example:ruby");

private CustomItemType registerRuby(PluginContext context) {
    var template = ItemTypes.of(ItemKey.PAPER).one()
            .withItemName(Component.text("Ruby", NamedTextColor.RED));

    return context.customItems().register(CustomItemType.builder(RUBY_ID, template.type())
            .components(template.components())
            .build()).type();
}

private void installRubyAssets(PluginContext context, CustomItemType ruby, byte[] rubyPng) {
    context.resourcePacks().create(PACK_ID, "Example custom content");
    var models = context.resourcePacks().models(PACK_ID);

    models.flatItem(ruby, Key.key("example:item/ruby"));
    models.texture(Key.key("example:item/ruby"), rubyPng);
}
```

`flatItem(...)` 使用 `minecraft:item/generated`，`handheldItem(...)` 使用
`minecraft:item/handheld`。两者都会生成：

```text
assets/example/items/ruby.json
assets/example/models/item/ruby.json
assets/example/textures/item/ruby.png
```

`texture(...)` 只校验 PNG 签名，不会缩放或修改图片。插件也可以用 `write(...)`、`writeJson(...)` 和
`rawModel(...)` 写任意合法资源包文件。

## 注册自定义方块和方块物品

下面使用 note block 的一个完整 state 作为 carrier。base state 会被 Fand 持续保护，避免邻居更新把逻辑
方块改回其它 note block state；默认情况下，note block 的交互和邻居行为也不会泄漏到自定义方块。

```java
import io.fand.api.block.BlockKey;
import io.fand.api.block.BlockTypes;
import io.fand.api.block.custom.CustomBlockContext;
import io.fand.api.block.custom.CustomBlockListener;
import io.fand.api.block.custom.CustomBlockRegistration;
import io.fand.api.block.custom.CustomBlockType;
import io.fand.api.component.DataComponentKey;
import io.fand.api.component.DataComponentMap;

private static final Key MACHINE_ID = Key.key("example:machine");
private static final DataComponentKey<Integer> ENERGY =
        DataComponentKey.integer(Key.key("example:energy"));

private CustomBlockRegistration registerMachine(PluginContext context) {
    var type = CustomBlockType.builder(MACHINE_ID, BlockTypes.of(BlockKey.NOTE_BLOCK))
            .state("instrument", "custom_head")
            .state("note", "0")
            .state("powered", "false")
            .components(DataComponentMap.of(ENERGY, 0))
            .mining(4.0F, 8.0F, BlockTypes.of(BlockKey.DIAMOND_BLOCK), true)
            .ticking(true)
            .build();

    return context.customBlocks().register(type, new CustomBlockListener() {
        @Override
        public void placed(CustomBlockContext event) {
            context.logger().info("Placed {}", event.type().id());
        }

        @Override
        public void tick(CustomBlockContext event) {
            var current = event.block().components().value(ENERGY).orElse(0);
            if (current < 100) {
                event.block().components().set(ENERGY, current + 1);
            }
        }

        @Override
        public void broken(CustomBlockContext event) {
            var stored = event.block().components().value(ENERGY).orElse(0);
            context.logger().info("Machine removed with {} energy", stored);
        }
    });
}
```

`placed`、`broken`、`loaded`、`unloaded` 和 `tick` 都在服务端线程调用。`broken` 执行时组件仍可读取，
回调完成后运行时才清理方块位置的数据。开启 `ticking` 的方块仅在所在区块已加载时逐 tick 回调；
不需要 tick 的方块不要启用它。

方块物品本身仍是一个 `CustomItemType`。注册两种类型后，通过 `bindItem(...)` 建立放置和掉落关系：

```java
private CustomItemType registerMachineItem(
        PluginContext context,
        CustomBlockRegistration machine
) {
    var template = ItemTypes.of(ItemKey.NOTE_BLOCK).one()
            .withItemName(Component.text("Machine", NamedTextColor.AQUA));

    var item = context.customItems().register(CustomItemType.builder(MACHINE_ID, template.type())
            .components(template.components())
            .build()).type();

    machine.bindItem(item.id());
    return item;
}
```

绑定后的玩家放置路径会：

1. 使用客户端包中的实际点击面选择目标方块。
2. 要求目标可替换，并执行 carrier 的生存和实体碰撞检查。
3. 放置逻辑方块，非创造模式消耗对应手中的一个物品。
4. 把绑定物品作为玩家正确破坏和程序化自然破坏时的默认掉落。

同一方块可绑定多个物品；默认掉落优先选择与方块逻辑 ID 相同的物品，否则按 item ID 稳定排序。
没有绑定物品的自定义方块默认不掉落物品。爆炸同样使用绑定掉落，并在
`DESTROY_WITH_DECAY` 模式下应用 vanilla 爆炸衰减。

## 挖掘属性和自定义工具

`CustomBlockMining` 的四个字段分别控制：

- `hardness`：实际破坏进度使用的硬度，`-1` 表示不可破坏。
- `blastResistance`：爆炸阻力。
- `toolRuleBase`：借用哪个 vanilla 方块的工具速度和正确掉落判定。
- `requiresCorrectTool`：是否必须被正确工具挖掘才掉落；为 `false` 时总是允许掉落。

例如把 `toolRuleBase` 设为 `minecraft:diamond_block`，会让 vanilla 工具按钻石块规则判断类型和等级，
但硬度和爆炸阻力仍使用自定义值。

`CustomItemType.Builder.tool(...)` 写入 vanilla `minecraft:tool` 组件，负责 vanilla 方块；
`customBlockToolRule(...)` 负责按逻辑 ID 或自定义标签覆盖 Fand 自定义方块。两者不是同一个规则表：

```java
import io.fand.api.item.component.ItemKeySet;
import io.fand.api.item.component.ItemTool;
import java.util.List;

private static final Key MACHINE_TAG = Key.key("example:mineable/machine_tool");

var vanillaTool = new ItemTool(
        List.of(ItemTool.Rule.minesAndDrops(
                ItemKeySet.tag(Key.key("minecraft:mineable/pickaxe")),
                8.0F)),
        1.0F,
        1,
        true);

var tool = context.customItems().register(CustomItemType.builder(
                Key.key("example:machine_tool"),
                ItemTypes.of(ItemKey.STICK))
        .tool(vanillaTool)
        .customBlockToolRule(ItemTool.Rule.minesAndDrops(
                ItemKeySet.of(MACHINE_ID),
                16.0F))
        .customBlockToolRule(ItemTool.Rule.overrideSpeed(
                ItemKeySet.tag(MACHINE_TAG),
                12.0F))
        .build()).type();
```

对应方块可以加入逻辑标签：

```java
var taggedMachine = CustomBlockType.builder(MACHINE_ID, BlockTypes.of(BlockKey.NOTE_BLOCK))
        .tag(MACHINE_TAG)
        .build();
```

`ItemTool.Rule.minesAndDrops(...)` 同时覆盖速度和正确掉落；`overrideSpeed(...)` 只覆盖速度；
`deniesDrops(...)` 只禁止正确掉落。规则按声明顺序匹配第一个。自定义标签只用于 Fand 逻辑方块规则，
不会把 carrier 动态加入 vanilla data-pack 标签；`CustomBlockType.is(BlockTagKey)` 仍委托给 base。

## 为 carrier 生成方块模型

原版客户端收到的是 `minecraft:note_block`，因此仅生成
`assets/example/blockstates/machine.json` 不会改变世界中的渲染。当前 carrier 模式应当：

1. 在插件命名空间生成自定义方块模型。
2. 在 `assets/minecraft/blockstates/<carrier>.json` 中把保留的完整 state 指向该模型。
3. 为所有其它 carrier state 提供正确的 vanilla fallback。

六面纹理模型可以直接生成：

```java
var models = context.resourcePacks().models(PACK_ID);
models.model(
        Key.key("example:block/machine"),
        Key.key("minecraft:block/cube"),
        Map.of(
                "down", Key.key("example:block/machine_down"),
                "up", Key.key("example:block/machine_up"),
                "north", Key.key("example:block/machine_north"),
                "south", Key.key("example:block/machine_south"),
                "west", Key.key("example:block/machine_west"),
                "east", Key.key("example:block/machine_east"),
                "particle", Key.key("example:block/machine_north")));

models.blockItem(machineItem, machine.type());
models.carrierBlockState(
        machine.type(),
        noteBlockStateValues(),
        Key.key("minecraft:block/note_block"));
```

`carrierBlockState(...)` 要求调用方提供 carrier 接受的每个属性和每个值，并保持 vanilla blockstate
使用的属性顺序。以下列表与当前测试插件使用的 note block carrier 一致：

```java
private static final List<String> NOTE_BLOCK_INSTRUMENTS = List.of(
        "harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar", "chime",
        "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling",
        "trumpet", "trumpet_exposed", "trumpet_oxidized", "trumpet_weathered", "zombie",
        "skeleton", "creeper", "dragon", "wither_skeleton", "piglin", "custom_head");

private static LinkedHashMap<String, List<String>> noteBlockStateValues() {
    var values = new LinkedHashMap<String, List<String>>();
    values.put("instrument", NOTE_BLOCK_INSTRUMENTS);
    values.put("note", java.util.stream.IntStream.rangeClosed(0, 24)
            .mapToObj(Integer::toString)
            .toList());
    values.put("powered", List.of("false", "true"));
    return values;
}
```

每张纹理通过 `models.texture(key, pngBytes)` 写入。也可以像测试插件一样把 PNG 和 JSON 放在插件 jar
的 `assets/<plugin-id>/...` 下，再用 `ResourcePackService.write(...)` 复制。当前六面贴图和完整示例见
[`ExampleAssets.java`](../test-plugin/src/main/java/io/fand/testplugin/ExampleAssets.java)。

`carrierBlockState(...)` 每次都会重写该 carrier 的完整 blockstate 文件。如果同一个资源包内有多个
自定义方块共享同一 vanilla carrier，不能连续调用它并期望自动合并；应选择不同保留 state，构造一份
合并后的 `JsonObject` 后调用 `blockState(...)` 一次，或为这些内容统一生成完整 variant 表。多个插件
覆盖同一个 `assets/minecraft/blockstates/...` 也会产生资源包优先级冲突。

该辅助方法会把所有非自定义 state 都指向同一个 `fallbackModel`。这对 note block 这种普通 state 共用
同一模型的 carrier 合适；楼梯、原木、栅栏等依赖 state 选择模型、旋转或 multipart 的方块不能靠一个
fallback 保留原版外观，应读取目标版本的 vanilla blockstate 结构并用 `blockState(...)` 生成等价定义。

保留的 carrier state 不应在普通玩法中自然出现。客户端只看 blockstate，无法判断该位置是否有 Fand
逻辑组件；普通 vanilla 方块如果碰巧进入同一 state，也会显示自定义模型，但服务端不会把它识别为
自定义方块。

## 构建和发送资源包

模型和纹理写完后构建 ZIP，并创建 vanilla 资源包请求：

```java
var build = context.resourcePacks().build(PACK_ID);
var url = context.resourcePacks().hostedUrl(build).orElseThrow(() ->
        new IllegalStateException("Enable network.resourcePacks.enabled in fand.yml"));
var request = build.request(
        url,
        true,
        Component.text("This server uses custom content"));

player.sendResourcePack(request);
```

也可以用 `context.resourcePacks().request(PACK_ID, required, prompt)` 完成构建、托管 URL 获取和请求创建。
自动托管由 `network.resourcePacks.enabled` 控制；公网、NAT 或反向代理环境应配置
`network.resourcePacks.publicBaseUrl`。构建同一个 pack ID 会按 pack 串行并以原子替换发布 ZIP。

插件作用域 pack ID `content` 在服务端实际保存为 `<plugin-id>.content`。`assets/` 树中的文件只能写入
当前插件的 `assets/<plugin-id>/...` 或用于 carrier 覆盖的 `assets/minecraft/...`；`pack.png` 等合法的
资源包根文件仍可写入。

## 程序化放置、移除和查询

```java
void placeAndInspect(PluginContext context, World world, int x, int y, int z) {
    var target = world.blockAt(x, y, z);

    // 直接设置 carrier、逻辑 ID 和本次实例组件。
    context.customBlocks().place(
            target,
            MACHINE_ID,
            DataComponentMap.of(ENERGY, 50));

    var type = context.customBlocks().customBlock(target);
    var allInChunk = context.customBlocks().customBlocks(world, x >> 4, z >> 4);
    var tickingInChunk = context.customBlocks().tickingBlocks(world, x >> 4, z >> 4);
}

// 方案 A：仅移除 Fand 逻辑身份和持久组件，carrier 仍留在世界中。
boolean clearLogicalIdentity(PluginContext context, Block target) {
    return context.customBlocks().remove(target);
}

// 方案 B：对仍保有逻辑身份的方块执行真正破坏，并使用自定义掉落钩子。
boolean destroyCustomBlock(Block target) {
    return target.breakNaturally(true);
}
```

`place(...)` 是直接世界操作，不执行绑定物品路径中的“目标可替换、玩家实体碰撞、玩家手中物品消耗”
策略。管理命令或世界生成器可以直接使用；模拟玩家放置时，调用方必须自行决定目标是否合法。

`remove(...)` 不是 break。它只触发 `broken`、清除逻辑身份和全部位置组件、停止 tick，然后保留
vanilla carrier。需要删除世界方块时使用 `Block.breakNaturally(...)` 或明确设置新的 block type。

## 选择 base 和 carrier

自定义物品和方块都不受固定 base 列表限制，但选择会影响实际行为：

- 物品 base 决定 vanilla 默认组件、堆叠上限、装备/使用入口和未覆盖的标签行为。
- 方块 carrier 决定碰撞形状、方块实体、光照、流体、可生存性和客户端 blockstate 入口。
- 自定义方块的硬度、爆炸阻力和正确工具规则由 `CustomBlockMining` 覆盖。
- `inheritBaseBehavior` 默认为 `false`，会隔离 carrier 的交互和邻居更新行为。只有确实需要 base 行为时才设置
  `.inheritBaseBehavior(true)`。
- Fand 目前没有自定义碰撞形状注册；需要不同形状时应选择具有目标形状的 carrier。
- 使用带方块实体的 carrier 时，逻辑方块也会拥有该 vanilla 方块实体；插件只能通过 Fand 已公开的
  `BlockEntity` 类型访问它。

选择 carrier 前应同时检查资源包 state 是否可安全保留、碰撞是否合适、是否会自然生成、是否有副作用，
以及其它插件是否已占用相同 state。

## 完整资源结构

一个包含独立物品、方块物品和六面方块模型的插件资源可以组织为：

```text
src/main/resources/
├─ fand-plugin.json
├─ assets/example/items/ruby.json
├─ assets/example/items/machine.json
├─ assets/example/models/item/ruby.json
├─ assets/example/models/block/machine.json
├─ assets/example/textures/item/ruby.png
├─ assets/example/textures/block/machine_down.png
├─ assets/example/textures/block/machine_up.png
├─ assets/example/textures/block/machine_north.png
├─ assets/example/textures/block/machine_south.png
├─ assets/example/textures/block/machine_west.png
└─ assets/example/textures/block/machine_east.png
```

`assets/minecraft/blockstates/note_block.json` 通常不直接放进插件 jar，而是在构建托管资源包时由
`carrierBlockState(...)` 生成。逻辑类型、模型 key 和选定 carrier state 因而可以由同一段注册代码维护；
carrier 的完整属性值列表仍需要随目标 Minecraft 版本核对。

## 常见错误

- 只注册 `CustomItemType`，但没有生成对应 `assets/<namespace>/items/<id>.json`：物品有逻辑身份，
  客户端却显示缺失模型。
- 为逻辑方块 ID 调用 `simpleBlockState(...)`，却没有覆盖实际 vanilla carrier blockstate：世界方块不会使用
  逻辑 ID 的 blockstate 文件。
- `carrierBlockState(...)` 只列出自定义 state，没有列全 carrier 的其它值：会让未列出的 vanilla state
  缺少模型。
- 连续为同一 carrier 调用 `carrierBlockState(...)`：后一次覆盖前一次。
- 设置 `requiresCorrectTool=true`，但没有正确的 `toolRuleBase` 或自定义工具规则：玩家能挖掉方块但得不到掉落。
- 忘记 `bindItem(...)`：物品不能走自动放置路径，方块也没有默认掉落。
- 把 `remove(...)` 当成破坏：逻辑数据被清除，但 vanilla carrier 仍存在。
- 开启 `inheritBaseBehavior` 后仍期待 note block、容器或红石 carrier 完全没有 vanilla 副作用。
- 从异步任务直接读写 `block.components()`：持久组件要求在服务端线程访问。

## 源码对应

本文档的行为分别由以下当前源码和测试约束：

- 物品类型与工具规则：
  [`CustomItemType.java`](../fand-api/src/main/java/io/fand/api/item/custom/CustomItemType.java)、
  [`ItemTool.java`](../fand-api/src/main/java/io/fand/api/item/component/ItemTool.java)。
- 方块类型、挖掘和生命周期：
  [`CustomBlockType.java`](../fand-api/src/main/java/io/fand/api/block/custom/CustomBlockType.java)、
  [`CustomBlockRegistry.java`](../fand-api/src/main/java/io/fand/api/block/custom/CustomBlockRegistry.java)。
- 资源包模型生成：
  [`ResourcePackModelGenerator.java`](../fand-api/src/main/java/io/fand/api/resourcepack/ResourcePackModelGenerator.java)。
- 服务端身份、放置、carrier state、工具、掉落和爆炸实现：
  [`FandCustomItemRegistry.java`](../fand-server/src/main/java/io/fand/server/item/FandCustomItemRegistry.java)、
  [`FandCustomBlockRegistry.java`](../fand-server/src/main/java/io/fand/server/block/FandCustomBlockRegistry.java)。
- 行为测试：
  [`FandCustomItemRegistryTest.java`](../fand-server/src/test/java/io/fand/server/item/FandCustomItemRegistryTest.java)、
  [`FandCustomBlockRegistryTest.java`](../fand-server/src/test/java/io/fand/server/block/FandCustomBlockRegistryTest.java)、
  [`FandResourcePackServiceTest.java`](../fand-server/src/test/java/io/fand/server/resourcepack/FandResourcePackServiceTest.java)。
