# 动态分区实施记录：P0/P1

日期：2026-09-05

对应方案：[动态分区与多线程化企划案](REGIONIZED_MULTITHREADING_PROPOSAL.md)

## 当前完成范围

已实现**现有串行执行模型上的目标调度入口**，以及**可独立执行的分区拓扑、holder 生命周期适配、区域执行器与局部 tick 队列**。
审计已扩展到区块、距离管理、POI 和实体存储的异步数据发布入口。
`MinecraftServer` 仍在同一个线程上依次推进所有世界；模型尚未接管 NMS 状态。局部时钟现已在内部执行模型中实现，尚未接到原版计划刻。
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

## 下一批实施顺序

1. 将 holder 适配器挂接到 NMS 的创建、复用、最终卸载和异步安装入口，覆盖 `pendingUnloads` 与保存等待生命周期。
2. 建立 NMS 状态容器与模型归属的一致发布协议，先在真实串行 tick 路径中验证，不能把独立模型误当成实时世界授权。
3. 将目标调度路由接到已验证的运行时归属，把局部时钟与延迟迁入原版计划刻，再补实体移交和跟随定时任务。
4. 测量大区域拓扑调整和任务队列开销，再实现有限预算的布局维护与负载策略。
5. 使用固定存档和插件构建实际集成基线；通过后才开启两个独立区域的实验并行。
