# Fand 红石 JIT 企划案

本文档记录 Fand 红石 JIT 的目标、边界、架构和落地顺序。这里的 JIT 不是重写一套新的红石规则，
也不是对电路做稳态求解，而是把 vanilla 红石执行过程编译成更快的有序执行器。

核心约束只有一句话：**红石顺序不变，生电机器可用，vanilla/Paper 语义不变。**

## 目标

- 在不改变红石行为的前提下，大幅降低热点红石区域的重复计算、对象分配和调度开销。
- 对大型生电机器保持兼容，包括 0-tick、活塞链、观察者链、比较器、漏斗/容器比较、铁轨和计划刻。
- 对动态世界保持正确：玩家放置/破坏方块、活塞移动、插件改方块、区块加载/卸载、结构生成和爆炸都能使缓存失效。
- 支持渐进启用：先观测，再 shadow 对比，再解释执行，最后只对稳定热点区域启用 hot executor。
- 出错时可自动降级，不让优化路径成为红石崩坏点。

## 非目标

- 不采用 Alternate Current 式传播算法替换。
- 不重排 neighbor update。
- 不合并或跳过 scheduled tick。
- 不把“看起来重复”的 update 当作无效 update 直接删除。
- 不为了跑分改掉 vanilla 的边界行为。

## 基本定义

### RedstoneRuntime

全局红石优化运行时，负责配置、采样、region 管理、shadow 校验、熔断和指标导出。

建议包名：

```text
io.fand.server.redstone
```

### RedstoneRegion

红石热点区域的动态快照，不是永久结构。它记录：

- region 内节点坐标和方块类型；
- chunk 加载状态和版本；
- 边界输入和边界输出；
- block entity 版本，尤其是比较器相关容器；
- dirty bitset；
- generation，用于 guard 快速判断。

region 可以跨 chunk，但必须有明确边界。边界外的变化只能通过 guard 或 dirty 标记进入执行器。

### RedstoneIR

对 vanilla 红石执行过程的中间表示。IR 编译的是“如何按 vanilla 顺序执行”，不是“最终电平结果”。

IR 节点可以表达：

- neighbor update；
- shape update；
- wire power calculation；
- comparator output calculation；
- diode/repeater state transition；
- observer pulse；
- piston action boundary；
- scheduled tick boundary；
- plugin/event boundary。

### RedstoneExecutor

执行 IR 的引擎。至少有三类：

- vanilla executor：原始路径，永远保留；
- warm interpreter：解释执行 IR，严格保序，便于调试和 shadow；
- hot executor：对稳定 region 使用更低分配、更少虚调用的专用执行路径。

## 配置模式

建议配置项：

```toml
performance.redstoneJitMode = "off"
```

可选值：

- `off`：完全关闭。
- `profile`：只采样和统计，不构建可执行 IR。
- `shadow`：构建 IR，并在 vanilla 执行后对比结果，不接管行为。
- `interpreter`：允许 warm interpreter 接管通过校验的区域。
- `hot`：允许 hot executor 接管稳定热点区域。

默认应先使用 `off` 或 `profile`。早期测试服可以用 `shadow` 找行为差异。

## 语义红线

红石 JIT 必须保留下列顺序和副作用：

- `NeighborUpdater` 的入队和出队顺序；
- `CollectingNeighborUpdater` 的递归限制和延迟执行语义；
- 方块更新时的事件触发点；
- `BlockRedstoneEvent` 等插件事件的触发顺序和可修改结果；
- scheduled tick 的 tick 边界；
- 活塞推出、回收、移动方块、掉落方块和 tile entity 搬迁顺序；
- 比较器读取容器、方块实体和特殊方块输出的时机；
- observer 对状态变化的检测时机；
- rail、target、sensor 等特殊红石部件的 vanilla 行为。

任何 guard 失败、事件取消、事件修改结果或动态边界不确定时，都必须 fallback 到 vanilla。

## 动态边界

下列情况必须让相关 region dirty 或直接熔断：

- chunk load/unload；
- 方块放置、破坏、替换；
- API `setBlock` 和批量方块操作；
- 插件事件修改红石输出或取消行为；
- 活塞开始移动、移动中、结束移动；
- block entity 内容变化，尤其是容器和比较器相关状态；
- observer 可见状态变化；
- 流体更新、爆炸、结构生成、世界生成；
- scheduled tick 注册或执行；
- 跨 region 的边界输入变化。

活塞是特殊高风险边界。第一版可以在活塞移动期间局部降级，移动路径标 dirty，移动结束后局部重建。

## Guard 设计

每次执行优化路径前，必须检查 guard：

- region generation 未变化；
- 相关 chunk 仍加载，chunk version 未变化；
- 节点方块 state id 和 block 类型未变化；
- block entity version 未变化；
- 边界输入快照未变化；
- 没有未处理的 plugin/event boundary；
- 当前执行入口和 vanilla 入口一致；
- 当前 tick 阶段允许使用该 executor。

guard 失败时不应报错，直接走 vanilla 或 warm interpreter。连续失败过多的 region 应短期禁用 JIT。

## Shadow 校验

`shadow` 模式必须做到：

- vanilla 仍然是唯一真实执行路径；
- JIT 在旁路记录预期输出；
- 对比最终 block state、电平、计划刻、事件结果和边界输出；
- mismatch 时记录最小必要诊断信息；
- mismatch 后熔断该 region，并保留可复现的 region id、入口坐标、触发方块和 step。

日志不能一股脑刷屏。需要聚合：

- 首次 mismatch 详细日志；
- 后续同 region 限流；
- 命令或 debug dump 才输出完整 IR。

## 分阶段落地

### 阶段 1：Profiler 和入口埋点

目标：不改变行为，只找热点。

工作项：

- 新增 `RedstoneRuntime` 骨架。
- 新增 `performance.redstoneJitMode` 配置。
- 在红石核心入口埋轻量 hook：
  - `NeighborUpdater`；
  - `CollectingNeighborUpdater`；
  - `RedStoneWireBlock`；
  - `DefaultRedstoneWireEvaluator`；
  - `ComparatorBlock`；
  - `DiodeBlock` / `RepeaterBlock`；
  - `ObserverBlock`；
  - `PistonBaseBlock`。
- 统计热点：
  - neighbor update 次数；
  - wire power calculation 次数；
  - comparator calculation 次数；
  - piston action 次数；
  - 单 tick 红石耗时；
  - top positions / top chunks / top regions。

验收：

- 默认关闭时开销接近 0。
- `profile` 模式不改变任何游戏行为。
- 可以定位高频红石热点区域。

### 阶段 2：Region 跟踪和 Dirty 系统

目标：建立可失效的动态区域模型。

工作项：

- 实现 `RedstoneRegion`。
- 实现 region 发现和合并策略。
- 接入 chunk load/unload dirty。
- 接入 block state 变化 dirty。
- 接入 block entity version。
- 接入 API setBlock / 批量 setBlock dirty。
- 接入活塞路径 dirty。

验收：

- region 不执行优化，只记录和失效。
- 所有动态变化能正确命中相关 region。
- 大型机器不会因为 region 扩张导致全图扫描。

### 阶段 3：Shadow IR

目标：生成 IR 并旁路对比。

工作项：

- 定义 `RedstoneIR` 节点类型。
- 从热点 region 编译 IR。
- 在 vanilla 执行后进行 shadow 对比。
- 实现 mismatch 熔断和限流日志。
- 增加最小 debug dump。

验收：

- `shadow` 模式不接管行为。
- 常见红石机器 shadow 无 mismatch。
- mismatch 能定位到 region、节点和触发入口。

### 阶段 4：Warm Interpreter

目标：让低风险热点区域由解释器接管。

工作项：

- 实现 `RedstoneInterpreterExecutor`。
- 每次执行前跑 guard。
- guard fail fallback vanilla。
- plugin/event boundary 默认 fallback vanilla。
- 对 wire、repeater、comparator、observer 的稳定子集先启用。

验收：

- 行为与 vanilla 一致。
- 解释器模式能降低热点区域对象分配和重复查找。
- mismatch 自动熔断，不影响服务器继续运行。

### 阶段 5：Hot Executor

目标：对长期稳定热点区域启用更激进的执行路径。

工作项：

- 对稳定 IR 做专用 executor。
- 减少虚调用、临时对象、坐标对象和 map 查找。
- 对边界输入使用紧凑快照。
- 对稳定节点使用 state id 快速路径。
- 实现热度衰减和失效重编译。

验收：

- 只对 shadow 和 interpreter 长期稳定的 region 启用。
- region 动态变化后快速回退。
- 生电测试集无行为差异。

### 阶段 6：覆盖面扩展

目标：扩大支持范围，而不是牺牲正确性。

候选扩展：

- 活塞链深度优化；
- rail；
- target block；
- sculk sensor / calibrated sculk sensor；
- crafter；
- copper bulb；
- 特殊容器比较器输出；
- 大型跨 chunk 机器。

每个扩展都必须先 shadow，再接管。

## 测试计划

需要建立红石兼容测试集：

- vanilla 小电路：线、火把、中继器、比较器、观察者。
- 计划刻：中继器延迟、observer pulse、按钮/压力板。
- 活塞：普通推动、粘性回拉、方块实体、不可推动方块、掉落方块。
- 比较器：容器内容变化、讲台、物品展示框、特殊方块。
- 0-tick 和生电常用机器。
- 跨 chunk 红石线和跨 chunk 活塞。
- 插件事件修改红石输出。
- 插件批量改方块后立即触发红石。

测试层级：

- 单元测试：region dirty、guard、IR 编译结构。
- 集成测试：固定红石电路 tick 后状态对比。
- Shadow soak：长时间跑大型机器，统计 mismatch。
- 性能测试：对比 vanilla path、interpreter、hot executor。

## 代码落点

NMS 入口参考：

```text
fand-server/src/minecraft/java/net/minecraft/world/level/redstone/NeighborUpdater.java
fand-server/src/minecraft/java/net/minecraft/world/level/redstone/CollectingNeighborUpdater.java
fand-server/src/minecraft/java/net/minecraft/world/level/redstone/InstantNeighborUpdater.java
fand-server/src/minecraft/java/net/minecraft/world/level/redstone/RedstoneWireEvaluator.java
fand-server/src/minecraft/java/net/minecraft/world/level/redstone/DefaultRedstoneWireEvaluator.java
fand-server/src/minecraft/java/net/minecraft/world/level/block/RedStoneWireBlock.java
fand-server/src/minecraft/java/net/minecraft/world/level/block/ComparatorBlock.java
fand-server/src/minecraft/java/net/minecraft/world/level/block/DiodeBlock.java
fand-server/src/minecraft/java/net/minecraft/world/level/block/RepeaterBlock.java
fand-server/src/minecraft/java/net/minecraft/world/level/block/ObserverBlock.java
fand-server/src/minecraft/java/net/minecraft/world/level/block/piston/PistonBaseBlock.java
```

Fand runtime 侧建议新增：

```text
fand-server/src/main/java/io/fand/server/redstone/RedstoneRuntime.java
fand-server/src/main/java/io/fand/server/redstone/RedstoneJitMode.java
fand-server/src/main/java/io/fand/server/redstone/RedstoneRegion.java
fand-server/src/main/java/io/fand/server/redstone/RedstoneRegionManager.java
fand-server/src/main/java/io/fand/server/redstone/RedstoneIR.java
fand-server/src/main/java/io/fand/server/redstone/RedstoneExecutor.java
fand-server/src/main/java/io/fand/server/redstone/RedstoneInterpreterExecutor.java
fand-server/src/main/java/io/fand/server/redstone/RedstoneProfiler.java
```

NMS 修改必须走 paperweight patch 工作流：

1. 修改 `fand-server/src/minecraft/java/**`。
2. 在内层仓库提交。
3. 运行 patch rebuild。
4. 外层提交前必须确认。

## 风险

- 红石顺序极其敏感，任何“等价优化”都可能破坏生电机器。
- 活塞和比较器是最高风险区域，应晚于基础 wire/repeater/observer 接管。
- 插件事件会让局部计算不可纯化，必须保守处理。
- region 过大会导致重编译和 guard 成本过高。
- region 过小会导致边界检查过多，收益不足。
- shadow 对比如果日志过量，会影响性能并污染控制台。

## 第一轮实现建议

第一轮只做阶段 1，不接管红石：

- 配置：`performance.redstoneJitMode`。
- 运行时：`RedstoneRuntime` + `RedstoneProfiler`。
- NMS hook：只记录热点和耗时。
- 命令或日志：输出 top 红石热点。

这样能先知道 Fand 真实红石瓶颈在哪里，再决定第二阶段 region 边界怎么切。后续每一阶段都能单独打
patch、单独回滚、单独验证，避免一次性把红石语义风险拉满。
