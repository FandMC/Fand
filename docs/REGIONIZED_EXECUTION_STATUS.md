# 动态分区实施记录：P0/P1

日期：2026-09-05

对应方案：[动态分区与多线程化企划案](REGIONIZED_MULTITHREADING_PROPOSAL.md)

## 当前完成范围

已实现**现有串行执行模型上的目标调度入口**，以及**可独立执行的分区拓扑、holder 生命周期适配、区域执行器与局部 tick 队列**。
第四批将 holder 生命周期和区块异步安装身份校验接入真实 NMS 路径；审计已扩展到距离管理、POI 和实体存储。
`MinecraftServer` 仍在同一个线程上依次推进所有世界；区域模型尚未接管 NMS 模拟状态。局部时钟现已在内部执行模型中实现，尚未接到原版计划刻。
P0/P1 整体尚未完成，不能把模型测试中的并行执行当作 Minecraft 世界已经并行。

新增公开入口：

```java
scheduler.at(location).run(action);
scheduler.forEntity(entity).call(query);
scheduler.global().run(action);
```

返回类型与使用规则见 [API 开发文档](API_DEVELOPMENT.md#所有者调度)。新增方法使用 `default`，不要求旧 `Scheduler` 实现立即补充抽象方法；
旧实现未提供这些能力时明确报告不支持，不会偷偷改用后台线程。
本批只增加单次调用，延迟、重复调度和区域所有权查询还没有作为公共 API 发布。

## 实际 tick 调用链

以下顺序来自当前 26.2 源码，不是未来区域架构的理想调用图：

```text
MinecraftServer.processPacketsAndTick()
  packetProcessor.processQueuedPackets()
  FandServer.tickScheduler()
    TaskScheduler.tick()
    recipes.tick(), maps.tick(), modProtocols.tick()
    commandTrees.tick() -> 给玩家发送更新后的指令树
    遍历世界 -> customBlocks.tick(world)
  MinecraftServer.tickServer()
    处理空服暂停、服务器 tick 计数和 tickRateManager
    MinecraftServer.tickChildren()
      commandFunctions、clockManager、时间同步
      遍历世界 -> ServerLevel.tick()
        世界时间、天气/睡眠和计划刻
        袭击、区块模拟、block events
        龙战、实体、方块实体和实体管理
      连接、玩家列表、调试与其他 tickable
      区块发送与网络 flush
    状态发布、自动保存与统计
```

源码：[MinecraftServer.java](../fand-server/src/minecraft/java/net/minecraft/server/MinecraftServer.java)、
[FandServer.java](../fand-server/src/main/java/io/fand/server/FandServer.java)、
[ServerLevel.java](../fand-server/src/minecraft/java/net/minecraft/server/level/ServerLevel.java)。

发现一个不能忽略的时钟边界：Fand 任务先于 `tickServer()` 的空服暂停判断运行。
因此，当前调度器 tick 不等于一次已经完成的世界模拟 tick。未来添加区域延迟时必须明确区分二者，不能直接复用同一计数器。
本批的新单次任务复用现有调度顺序，不改变这个已有行为。

## 首批共享状态清单

| 位置与状态 | 当前归属 | 区域化前必须解决的事项 |
| --- | --- | --- |
| `ServerLevel.entityTickList`、玩家列表 | 世界线程 | 分区实体索引、乘客共同归属、移交和去重 |
| `ServerLevel.blockTicks` / `fluidTicks` | 世界线程 | 按区域分配队列，拆分/合并转换局部截止时间 |
| `ServerLevel.blockEvents` / `blockEventsToReschedule` | 世界线程 | 保持顺序与同 tick 副作用，不以异步消息替代 |
| `Level.blockEntityTickers` / `pendingBlockEntityTickers` | 世界线程 | 延迟加入和当前遍历必须属于同一执行域 |
| `Level.neighborUpdater` | 世界线程 | 链式更新范围和所有权缓冲，避免修改一半后才发现越界 |
| `Level.random` 使用 `ThreadUnsafeRandom` | 世界线程 | 随机源局部化与序列兼容决定，禁止多个区域共享推进 |
| `Level.thread` | 创建世界时的线程 | 从固定线程身份迁移到执行所有权 |
| `ServerChunkCache` 距离管理、票据、`spawningChunks`、刷怪统计 | 世界线程 | 加载状态发布、临时列表分区和世界总额协调 |
| `ServerLevel` 爆炸缓存、每 tick TNT 额度 | 世界线程 | 缓存覆盖范围/生命周期与全世界额度含义 |
| 世界时间、`clockManager`、袭击、龙战、睡眠 | 世界/服务器控制流程 | 每项确定唯一决策者，避免每区重复更新 |
| Fand 自定义方块和组件 | 全局任务中逐世界运行 | 按方块位置迁移 tick 与组件访问 |
| 配方、地图、协议、指令树更新 | Fand 全局任务 | 注册表版本发布与按玩家所有者发送分开 |

补充源码：[Level.java](../fand-server/src/minecraft/java/net/minecraft/world/level/Level.java)、
[ServerChunkCache.java](../fand-server/src/minecraft/java/net/minecraft/server/level/ServerChunkCache.java)。
这是入口级审计，不是整个 NMS 的共享状态审计已经完成。第二批继续检查的发布入口如下：

| 系统 | 确认的共享状态/完成入口 | 接入要求 |
| --- | --- | --- |
| `ChunkMap` | `updatingChunkMap`、`visibleChunkMap`、`pendingUnloads`、实体跟踪和玩家预加载引用；`scheduleChunkLoad()` 在后台解析后回 `mainThreadExecutor` 安装区块 | 区分后台计算和实时对象发布；区块及 POI 安装必须在合法所有者中执行 |
| `ChunkMap.scheduleUnload()` | 等待保存同步 Future，再经 `unloadQueue` 移除 holder、保存、卸载世界区块并更新光照 | 保留 holder 身份检查，协调 cell 最后一个活动区块的撤销和在途保存 |
| `DistanceManager` | `playersPerChunk`、票据集合、距离传播与 `ChunkHolder.updateFutures()`；部分异步完成后再次投递主线程 | 分离世界级统计和按位置状态；不能只换成并发 Map 后让多个区域共同更新 |
| `PoiManager` | `loadedChunks`、村庄距离追踪、`take()` / `release()` 与脏状态发布 | POI 占用和修改随空间所有者推进；存储层加载完成不等于可跨线程直接调用 |
| `PersistentEntitySectionManager` | UUID 集合、区块可见性、加载状态和 `loadingInbox`；`processPendingLoads()` 安装实体并更新状态 | 并发队列只保护交接；消费时的实体安装、去重和区块状态必须一起归属到正确执行域 |

源码：[ChunkMap.java](../fand-server/src/minecraft/java/net/minecraft/server/level/ChunkMap.java)、
[DistanceManager.java](../fand-server/src/minecraft/java/net/minecraft/server/level/DistanceManager.java)、
[PoiManager.java](../fand-server/src/minecraft/java/net/minecraft/world/entity/ai/village/poi/PoiManager.java)、
[PersistentEntitySectionManager.java](../fand-server/src/minecraft/java/net/minecraft/world/level/entity/PersistentEntitySectionManager.java)。

另发现 `ChunkMap.handleChunkLoadFailure()` 对部分失败会继续创建空区块。它不能直接作为区域所有权失败的兜底策略；
解析失败、已有存档损坏和过期所有者结果应分别处理。本批记录该路径，没有改动既有区块恢复行为。

## API 调用分类与迁移顺序

| 类别 | 当前实例 | 后续处理 |
| --- | --- | --- |
| 所有者内可同步完成 | 方块读写、当前实体状态与库存操作 | 保持主体签名，把线程许可改为目标所有权 |
| 已有 Future 的操作 | 玩家传送、世界时间修改、部分实体动作 | 保留签名，逐项接入目标所有者和生命周期协议 |
| 阻塞投递 | `ServerThreading.callBlocking()`、`FandBlock.callOnServerThread()`、部分玩家方法的 `join()` | 明确同步调用范围，跨所有者提供异步替代 |
| 自动投递的 `void` 操作 | `FandEntity.runOnServerThread()` 和玩家/方块包装器 | 明确调用是立即完成还是只接受请求，避免悄悄改变行为 |
| 只读快照 | 玩家/世界列表和区块跟踪数据 | 区分不可变集合与其中可变对象；不自动授予所有权 |
| 必须单独设计 | 跨区库存、命令链、邻居更新、传送与存档 | 先定义原子边界，再接入区域调度 |

源码：[ServerThreading.java](../fand-server/src/main/java/io/fand/server/util/ServerThreading.java)、
[FandBlock.java](../fand-server/src/main/java/io/fand/server/block/FandBlock.java)、
[FandEntity.java](../fand-server/src/main/java/io/fand/server/entity/FandEntity.java)、
[FandPlayer.java](../fand-server/src/main/java/io/fand/server/entity/FandPlayer.java)。

`FandBlock` 还存在“server 为 null 就直接在调用线程执行”的分支；它不能被区域实现继承为缺失所有者时的兜底。
本批没有大面积改写这些原有 API，以免把尚未落实的区域语义提前带入现有玩法。

## 第一批：目标调度的约束

- 所有者任务与现有主线程任务共用 tick 队列，保持提交顺序；回调中提交的任务留到下次 tick。
- `ServerTickAccess` 在服务端执行线程上检查目标；任务提交线程不读取实时实体位置或世界注册表。
- 位置任务绑定世界实例。卸载后同 key 的新世界不能继承旧任务。
- 实体任务检查当前句柄、世界实例、移除状态与实体索引中的身份，不依赖 UUID 相等来恢复旧对象。
- 玩家包装器重新绑定时读取当前句柄；普通实体若被原版替换为新实例，旧包装器退休。跨实例身份移交仍属于后续 NMS 设计。
- Future 失败和取消对调用者可见。调度器关闭会结束待处理 Future，不能遗留永远不完成的查询。
- 通过插件上下文提交的查询进入资源追踪，结束后释放；停用后拒绝新提交，并在执行入口阻止尚未进入的动作。
- 取消回调在队列锁/资源锁外调用，避免回调再次调度时产生锁等待循环。
- 本批的停用语义仍是尽力取消；已经进入的插件回调不保证被排空。完整的在途计数、生命周期代际与类加载器静止协议尚未实施。

## 第二批：拓扑、执行上下文与 cell 任务队列

实现位于 [tick 包](../fand-server/src/main/java/io/fand/server/tick/TickRegionTopology.java)，未增加公共 `fand-api` 接口。

### 网格与布局

- `OwnershipCell` 使用固定网格坐标，区块坐标转换采用向下取整，覆盖负坐标。
- 每个已加载世界应拥有独立的 `TickRegionTopology` 实例。区域身份不跨该实例复用，同一个数值 ID 不代表同一个区域。
- 每个活动 cell 周围有配置好的缓冲范围；缓冲重叠的部分归入同一区域。缓冲 cell 本身不会因此变成活动 cell。
- `tryActivate()` 合并受影响的空闲区域；`tryDeactivate()` 只对受影响区域重新计算连通分量并拆分。其他区域的身份保持。
- 区域正在执行时，相关变更返回 `BUSY`，整个布局保持原样；holder 适配器会保留变更，后续维护 pass 按预算重试。当前没有动态负载策略。
- 重复激活或撤销非活动 cell 返回 `UNCHANGED`。缓冲坐标溢出在发布之前失败，不会绕回负坐标并修改其他区域。

模型的激活单位是 **cell**。未来区块适配器必须根据同一 cell 中的区块 holder 生命周期聚合首个加入与最后一个退出，
不能每卸载一个区块就撤销整个 cell。第三批已实现该适配器，真实 NMS 生命周期挂接仍未完成。
缓冲半径是模型参数，测试中的数值不是对 NMS 所有访问范围的安全证明，也尚未加入服务器配置。

### 执行上下文

`RegionContext` 是绑定当前线程的一次执行许可。进入时区域从 `READY` 变为 `RUNNING`，释放后才能进行布局调整。
不同区域可以由不同线程同时执行；同一区域不能同时进入，一个线程也不能同时持有两个区域。
上下文退出后不能继续检查/使用其所有权，旧上下文重复关闭不会释放后续执行获得的新许可。

拓扑替换创建新区域身份，旧身份变为 `RETIRED`，不能再次进入。关闭整个拓扑要求先释放所有执行许可，随后退役全部区域。
这个上下文目前只保护模型内的归属与任务执行；NMS 仍使用原来的线程模型，不得凭模型许可绕过现有线程限制。

### 任务路由与生命周期

任务存放在活动 cell 的队列中，执行区域在领取任务时确定。拆分或合并只更新所有权布局，保留仍活动 cell 的队列，
不搬运 Future、不保存旧区域转发链。跨 cell 合并后的任务按原提交序号执行。

撤销 cell 时，旧活动实例的待执行任务取消。重新激活同一坐标建立新队列，即使取消回调中立即重新激活，也不会复活旧任务或误取消新任务。
一次执行上下文只领取一批任务；执行期间的新提交留给下一次执行。取消会从待执行队列释放对应任务。

普通任务异常进入其 Future；严重 `Error` 会结束已经领取的剩余结果并向执行者抛出。
释放失败上下文时，区域转为 `FAILED`，未领取的任务也失败，后续执行和拓扑修改被阻止。
第三批的 `runTick()` 将此边界扩展到整个模拟回调；模拟回调中的普通异常也会使区域失败。
模型不尝试回滚回调已经产生的副作用，真实 NMS tick 仍需通过该执行边界接入。

布局发布和队列交接使用同一把短期元数据锁；任务本体和完成/取消回调在锁外执行。
当前连通分量重算和缓冲集合构建仍在该锁内，虽然仅涉及受影响区域，但成本会随大区域增大。
大规模 cell 的开销、调度公平性、背压与负载自适应需要后续基准验证，本批不宣称已解决它们。

## 第三批：holder 生命周期、执行派发与局部时钟

### holder 生命周期与异步结果

[ChunkOwnership](../fand-server/src/main/java/io/fand/server/tick/ChunkOwnership.java) 聚合同一 cell 的多个区块 holder，
维护线程固定为创建它的控制线程。其他线程只能提交注册、退休请求，或通过已激活的 handle 提交动作。

- `register(chunkX, chunkZ, holder)` 创建一次独立生命周期；同一坐标甚至同一 holder 对象再次注册，仍生成新身份。
- `ready()` 完成后才允许通过该 handle 投递。等待期间不得提前把新 holder 发布为实时可访问的区块。
- 第一个 holder 激活 cell，最后一个 holder 完成退休才撤销 cell。替换 holder 不需要重建仍活动的 cell 队列。
- `retire()` 立即阻止该身份接受新动作，实际撤销等所有者空闲。已经进入的动作允许结束，不执行线程中断。
- 同一坐标的新注册替换旧身份；旧身份迟到的退休请求不能卸载新 holder。
- `handle.submit()` / `submitAfter()` 的动作在执行时再次校验身份。退休也会取消尚未执行的该 holder 任务，即使 cell 仍有其他区块。
- 注册/退休返回的是观察 Future 的副本，取消副本不会偷偷取消区块生命周期操作。

`drain(maxRequests, maxChanges)` 分别限制读取请求数和尝试变更数。忙坐标轮转到末尾，其他独立区域仍可处理；
每个 pass 不阻塞等待区域，也不在循环内反复重试同一忙坐标。这限制的是工作项数量，单次大型连通分量重算仍可能较贵。

维护期间短暂停止新执行许可的领取，已有区域继续执行；涉及运行中区域的成员变化一律延期，包括同一 cell 内的 holder 增减。
此约束使 holder 身份切换与拓扑更新处于同一安全点。完成和取消通知在维护门禁与元数据锁之外运行，允许继续提交请求。

`shutdown()` 立即停止新执行许可和任务接收。控制线程继续 `drain()`，直到已接受请求收尾且所有执行许可释放，
再退休全部 holder、取消未执行动作并结束关闭 Future。失败区域也允许通过世界关闭统一回收。

### 区域执行与失败边界

[RegionTickExecutor](../fand-server/src/main/java/io/fand/server/tick/RegionTickExecutor.java) 使用调用方提供的工作线程池，
对每个区域身份最多保留一个待执行或执行中的 tick。不同区域之间没有全部完成后才能继续的屏障。

领取时重新检查区域身份：拓扑已经替换、区域忙、维护门禁关闭或世界正在停止时返回 `UNAVAILABLE`，交给控制流程重试。
它不依赖数值 region ID 区分世界；不同世界相同 ID 不会共用任务。
执行器关闭取消尚未开始的 tick，并等待已开始 tick 释放许可，不负责关闭调用方的共享线程池。

`RegionContext.runTick()` 先消费到期队列，再运行一次区域模拟。成功释放后才推进局部计数；
模拟失败会使整个区域进入 `FAILED`，未领取任务失败，时钟不前进。执行结果通知发生在释放区域许可之后。
单独调用 `runTasks()` 只执行队列，不代表完成一次世界模拟，因此不会推进时钟。

### 局部 tick 延迟

`submitAfter(cell, delayTicks, action)` 使用正整数局部 tick 延迟。运行中的 tick 内提交延迟，从该 tick 结束后开始计数。
合并选择来源区域中最大的局部计数，并按 `newDue = oldDue + targetNow - sourceNow` 平移每个来源的截止 tick；
拆分后的子区域继承父区域时钟，随后各自推进。截止时间溢出会在布局发布前失败，原所有权与队列保持完整。

cell 的队列按截止 tick 和提交序号排序，领取时只读取到期部分；不会每 tick 扫描所有远期任务。
取消从队列移除任务；合并时在重新索引之前完成全部截止时间验证。
这套时钟尚不包含墙钟延迟、世界游戏时间、冻结/变速与实体移交，也没有发布为新的公共 API。

### 接入边界

目前可以组合 holder 适配器、区域执行器与模型回调验证并行、合并/拆分和关闭。但**不能将回调直接替换成 `ServerLevel.tick()`**：
原版实体、计划刻、POI、距离管理等状态仍归世界线程。现有 `Scheduler.at()` / `forEntity()` 继续使用原串行实现。
执行器也尚未提供自动 20 TPS 定时、公平队列、队列总量背压或按负载自适应的区域调整。

## 第四批：接入真实 NMS holder 与异步加载

新增内部 [SerialChunkOwnership](../fand-server/src/main/java/io/fand/server/tick/SerialChunkOwnership.java)，
将模型生命周期挂接到 `ChunkMap`，使用现有世界控制线程完成注册、撤销和数据安装。
拓扑保持私有，不向区域工作线程开放执行许可。当前 cell 为 4 × 4 区块、缓冲半径为 1 个 cell，
仅用于记录真实 holder 对应的连通布局，不能据此认定原版邻域访问已经安全分区。

- 新建 `ChunkHolder` 时，先完成身份绑定，再放入 `updatingChunkMap`。从 `pendingUnloads` 取回旧 holder 时保留原身份。
- `scheduleUnload()` 的旧回调只有成功移除对应 holder 才能撤销身份；等待保存期间仍保持所有权。
  最终卸载即使没有生成出 chunk，也会撤销身份；有 chunk 时先完成保存提交和原版卸载操作。
- `scheduleChunkLoad()` 捕获本次 holder 的身份，后台继续读取和解析数据、预取 POI；回到原世界事件循环后，
  只有该身份仍然有效才执行安装或原有读取失败恢复。不会让旧结果写入同坐标的新 holder。
- 世界关闭先停止接收安装，再退休 holder。尚未完成 I/O 或已经排队的安装 Future 均会结束；
  未完成的来源 Future 只保留可解除的回调，退休后不再通过安装器引用整个世界，也不取消可能共享的底层 I/O。
- 专用 `RetiredChunkException` 在原版生成状态链中转为 `UNLOADED`，不会制造空区块或触发延迟崩溃。
  普通 I/O 错误继续走现有恢复流程，其他生成异常仍保留原版失败处理。

安装过程不进入 `RegionContext`，因为原版安装仍可能同步请求其他 cell，且同步请求会通过 `managedBlock()` 消费世界事件循环。
当前接入验证的是生命周期和发布顺序；实体、计划刻、POI 内部状态及距离管理仍归串行世界线程。
已经进入的安装回调允许执行完毕，不通过中断或回滚撤销副作用。

同时移除了原读取失败处理中的空条件分支及其无用变量、导入；没有扩大“遇到异常就生成空区块”的适用范围。

### 真实重载发现的生成状态混淆

隔离服首次生成、保存和卸载均成功，但同坐标重载卡在同步区块请求中。针对性回归复现了已有调度器的问题：
较小任务读取一个存档状态为 `FULL` 的区块后，把这个持久化状态当成当前运行时已经完成的加载状态，
因而清除了后续更高目标的请求，留下永远不完成的 Future。

`ChunkGenerationTask` 完成任务时现在报告本次 Future 实际对应的状态。存档中的高级状态只用于选择加载或生成步骤，
不能替代当前运行时仍需执行的光照初始化、光照加载和 FULL 转换。
同时在构建加载图之前捕获目标状态，避免构建期间发生目标升级后，把旧 Future 错标为新目标而跳过步骤。
两个回归分别验证持久化 FULL 区块的后续加载请求，以及加载图构建期间的目标升级；均已先观察到旧代码失败。

## 第一批验证结果

已有调度器与线程桥测试作为改造前基线；新增回归覆盖后台提交、提交顺序、下一 tick、异常传递、
取消、关闭与提交竞争、关闭时已经取出的任务、失效世界、实体移动/移除/替换、玩家句柄重绑定和插件停用。
关联回归同时覆盖 API、插件生命周期与指令系统。

第一批新增 24 个回归用例。验证结果为 API 198 项、相关服务端 176 项全部通过，零失败、零跳过；
独立 `test-plugin` 工程也已重新编译通过。执行命令：

```powershell
./gradlew.bat :fand-api:test :fand-server:test --tests "io.fand.server.scheduler.*" --tests "io.fand.server.plugin.*" --tests "io.fand.server.command.*" --tests "io.fand.server.util.ServerThreadingTest" --console=plain
./gradlew.bat -p "test-plugin" compileJava --console=plain
```

这批是调度器与包装器层面的真实实现测试，NMS 目标使用 mock 隔离服务器启动。
尚未用实际在线玩家验证区域并行，也尚无多核性能结论。正式的服务器集成场景、机器基准、旧插件二进制样本和所有权压力模型仍是 P0/P1 后续工作。

## 第二、三批验证结果

第二、三批统一验证已完成：API 198 项、服务端 717 项、已有集成检查 3 项全部通过，零失败、零跳过。
服务端测试包含 69 项区域模型测试，覆盖随机拓扑变更、并行执行、holder 替换竞争、延迟换算与关闭。
另外将 4 处依赖清理实现中局部变量名的源码断言移除，使用实际资源清理行为测试及已有服务注册表测试覆盖。

```powershell
./gradlew.bat :fand-api:test :fand-server:test :fand-server:integrationTest :fand-server:assemble --console=plain
```

完整构建成功，生成 `fand-server/build/libs/fand-server-0.8.4-clip.jar`，以及主 JAR、源码包和 Javadoc 包。
已有集成检查仅验证构建信息与源码挂接，不是在线玩家或真实分区世界的运行测试。

## 第四批验证结果

生命周期接入完成后，全量 API 198 项、服务端 733 项、已有集成检查 3 项通过；完整构建成功。
随后真实重载发现的两处生成调度错误已加入回归，117 项区域模型、区块生成和并行派发相关测试全部通过。
本批新增 18 项用例：串行生命周期与发布 13 项、NMS 状态链及事件循环 3 项、磁盘状态和目标升级 2 项。

修复后的定向验证与启动包构建：

```powershell
./gradlew.bat :fand-server:test --tests "io.fand.server.tick.*" --tests "net.minecraft.server.level.Chunk*Test" --tests "net.minecraft.server.level.FandAsync*Test" --tests "net.minecraft.server.level.FandChunk*Test" --tests "net.minecraft.server.level.ParallelChunkTaskDispatcherTest" :fand-server:fandclipJar --console=plain
```

NMS 变更已先提交内层 Git，再由 paperweight 生成 `0107`、`0108` 两个 feature 补丁。
本批仅新增 feature 提交，内层工作区干净，file/resource 层未改动，因此使用以下专用任务，避免无关的上游基线重建：

```powershell
./gradlew.bat :fand-server:rebuildFeaturePatches -x :fand-server:rebuildFilePatches --console=plain
```

有 file/resource 层改动时应使用完整重建流程，不能照搬上述排除项。

隔离服使用 Java 25、独立测试存档、仅本机监听、2 个区块生成线程和 2 个后台线程。
运行探针通过内部 NMS 快照读取 holder、cell 和区域数量，主世界验证过程如下：

| 操作 | holders | 活动 cells | regions |
| --- | ---: | ---: | ---: |
| 加载两个相隔较远的 4 × 4 区块组，含生成依赖邻域 | 1800 | 128 | 2 |
| 保存并移除全部强制加载，等待最终卸载 | 0 | 0 | 0 |
| 从磁盘重载原坐标 | 1800 | 128 | 2 |
| 只卸载其中一片，另一片继续活动 | 900 | 64 | 1 |

重载后使用 `execute if block` 验证手动写入的钻石块仍存在，输出 `OWNERSHIP_RELOAD_DATA_OK`。
带着剩余 900 个 holder 执行 `stop`，全部维度保存完成并输出 `Fand runtime stopped`，进程退出码为 0。
初次试运行暴露的卡住及修复经过见上文；这些结果不代表已验证在线玩家、实体跨区或多核吞吐。
本机 OSHI 读取 Windows 性能计数器的既有报错仍存在，与本批所有权及加载检查分开记录。

## 下一批实施顺序

1. 在已接入 holder 生命周期的基础上，将真实区块、实体与计划刻等状态容器按归属组织，建立一致发布协议。
2. 扩展串行运行中的所有权审计，覆盖 POI 和实体存储的异步发布；不能把独立模型误当成实时世界授权。
3. 将目标调度路由接到已验证的运行时归属，把局部时钟与延迟迁入原版计划刻，再补实体移交和跟随定时任务。
4. 测量大区域拓扑调整和任务队列开销，再实现有限预算的布局维护与负载策略。
5. 使用固定存档和插件构建实际集成基线；通过后才开启两个独立区域的实验并行。
