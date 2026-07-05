# Fand Server 项目企划书

## 项目概述

**项目名称**: Fand Server  
**项目类型**: 基于 Minecraft 原版服务端的增强型服务端  
**技术路线**: 类似 Bukkit/Spigot/Paper 的 patch 模式  
**目标版本**: Minecraft 26.1.2（最新版本）  
**开发语言**: Java  
**开发模式**: 个人全职投入  

---

## 核心定位

Fand 是一个**基于 Mojang 原版服务端代码**进行修改和增强的高性能 Minecraft 服务端实现，目标是：

1. **性能优化** - 在多线程、网络 I/O、区块加载等方面超越 Paper/Purpur
2. **现代化 API 设计** - 提供类型安全、易用、功能强大的全新插件 API
3. **特定功能增强** - 实现原版和现有服务端缺失或不完善的功能

**重要区别**：Fand 不是从零重写（如 Minestom/Glowstone），而是基于原版代码进行 patch 修改，确保原版游戏机制的完整性和兼容性。

---

## 技术架构

### 1. 代码获取方式

- **Paperweight**: 直接使用 Paper 官方的 paperweight-core 构建工具
- **Mache**: 基于 Mache 项目提供的反编译原版代码（26.1+ 已无混淆）
- **补丁系统**: 使用 Git patch 管理对原版代码的修改，与 Paper 相同机制
- **构建流程**: 完全自动化，从 piston-meta 获取 → unbundle → decompile → apply patches

### 2. API 设计理念

**不兼容 Bukkit API**，设计全新的现代化插件 API：

- **类型安全**: 充分利用 Java 泛型和类型系统
- **响应式/事件驱动**: 现代化的事件系统
- **流式 API**: 链式调用，提升开发体验
- **组件化架构**: 插件可以注册自定义组件和系统
- **协程支持**: 考虑集成 Kotlin 协程或 Project Loom 虚拟线程

### 3. 性能优化方向

- **异步区块加载**: 优化区块 I/O 和生成流程
- **实体优化**: 智能实体 tick 调度，减少无效计算
- **网络优化**: 批量包发送、压缩优化、协议缓存
- **内存优化**: 对象池、更高效的数据结构
- **多线程优化**: 合理利用多核 CPU（参考 Folia 的区域化设计）

### 4. 技术栈

```
Java 25 (最新版本，支持最新语言特性)
├─ Paperweight (构建工具)
├─ Mache (反编译源码)
├─ Netty (网络框架，原版自带)
├─ Caffeine (缓存)
├─ FastUtil (高性能集合)
├─ Adventure (现代化文本组件库)
├─ Logback (日志)
├─ JSpecify (空安全注解)
└─ JUnit 5 + AssertJ + Mockito (测试)
```

---

## 目标用户

- **小型私服** (1-20人): 初期主要验证功能完整性
- **中型社区** (20-100人): 中期目标，验证性能和稳定性
- **大型服务器** (100+人): 长期目标，证明架构的可扩展性
- **插件开发者**: 提供优秀的开发体验和完善文档

---

## 当前进度总览（2026-07-05）

Fand 已经越过“能否跑起来”的阶段。当前真实状态是：

- **基础设施**：已完成。Paperweight patch 流、模块划分、Fandclip 构建、版本发布和基础 CI 已可用。
- **核心 API**：进入后期。事件、插件生命周期、命令、调度、权限、配置、世界/方块/实体/玩家、物品、GUI、Packet、Recipe、Scoreboard、Region、Storage 等表面已有实现，下一步重点是文档、示例和稳定性。
- **性能优化**：已提前进入深水区。区块生成/加载、网络发送、启动路径、watchdog、爆炸同步读、实体/AI/流体/随机 tick 等热点已有多轮优化，但还需要系统化 benchmark 和长期稳定性验证。
- **生态**：刚起步。测试插件覆盖面较广，MiniMOTD 已完成 Fand 平台移植探索；真正的外部插件生态、作者数量和文档仍是短板。
- **红石 JIT**：已暂停并拆除。前期 profiling / shadow / wire hot path 尝试带来复杂度和行为风险，当前路线回到原版红石语义与技术服兼容优先，后续不把红石 JIT 作为近期目标。

---

## 开发路线图

### Phase 0: 基础设施搭建 (2-4 周) ✅ **已完成**

**目标**: 建立可持续的开发流程

- [x] 设计并实现 patch 系统（使用 paperweight）
- [x] 建立自动化构建流程
- [x] 反混淆和代码映射工具链（基于 Mache）
- [x] 项目结构和模块划分（fand-api / fand-server / fandclip / fand-data-generator）
- [x] 基础文档和编码规范（CODING_STANDARDS.md）
- [x] 基础 CI 流程（GitHub Actions）
- [ ] CI 集成测试、压力测试和发布矩阵

**产出**: 能够自动拉取原版代码、应用 patch、编译运行的基础框架 ✅

**当前状态**:
- 构建和 patch 管道完全可用，已支持 `rebuildPatches`、服务端构建与 Fandclip 启动
- 已有有序 vanilla feature patch 集，覆盖启动、命令命名空间、事件钩子、物品栏交互、配方同步、控制台、代理转发、TPS/MSPT 采样、watchdog、区块/网络/爆炸优化与服务器 GUI 主题
- 核心 API 接口已定义，并已有插件、事件、命令、调度、权限、配置、世界/方块、实体/玩家、背包/物品、GUI、Packet、Recipe、Scoreboard、Region、Storage 等运行时实现
- `fand-data-generator` 已接入构建，从原版注册表自动生成类型化 key API
- Fandclip 启动器已实现
- 仍需补齐 CI 集成测试、压力测试、发布矩阵和端到端服务端启动验证

---

### Phase 1: 核心 API 设计 (3-6 周) 🚧 **后期收敛**

**目标**: 定义插件 API 的核心接口和架构

- [x] **事件系统设计**
  - 优先级和取消机制 ✅
  - 类型安全的事件监听器 ✅
  - 异步事件支持 ✅
  
- [x] **插件生命周期管理**
  - 加载、启用、禁用接口定义 ✅
  - 基础依赖排序与插件 classloader 隔离 ✅
  - 失败路径和资源清理硬化（进行中）
  - 插件间通信机制（待实现）

- [x] **核心 API 接口**
  - Server 顶层接口 ✅
  - Command API 框架 ✅
  - Scheduler API 定义与基础实现 ✅
  - World/Block API 基础实现 ✅
  - Entity/Player API 基础实现 ✅
  - Inventory/Item API 基础实现 ✅
  - Scoreboard/Tab 基础 API ✅
  - Performance API 与 TPS/MSPT 命令 ✅
  - Recipe API ✅
  - Particle/Sound API ✅
  - Item Component/DataComponent API ✅
  - 类型化 vanilla registry key API ✅
  - ActionBar/Title/BossBar/Kick 等玩家展示能力 ✅
  - Command Builder 与注解命令 API ✅
  - Packet、Plugin Messaging、Placeholder、Region、Storage、Custom Item/Block 等扩展 API ✅

- [x] **配置系统**
  - YAML 支持 ✅
  - 类型安全的配置映射 ✅
  - 部分热重载支持 ✅
  - JSON/TOML 支持（按需再评估）

**当前进度快照（2026-07-05）**:
- 命令 API 已从旧 `CommandDescriptor + lambda` 过渡到 Builder 与注解驱动，保留独立命令类写法，适合真实插件组织。
- API 版本策略已明确：发行版本可继续前进，`PluginDescriptor.CURRENT_API_VERSION` 保持 `0.1.1` 作为插件描述协议版本。
- 事件、配置、权限、调度、GUI、Packet、Recipe、Scoreboard、Region、Storage、Custom Item/Block、NMS Hook 等运行时入口已经接入 `PluginContext`，插件禁用时的资源清理路径持续加固。
- MiniMOTD 已完成 Fand 平台移植探索，验证了第三方插件接入 FandPluginGradle、命令、事件和运行时依赖的基本路径。
- `docs/API_DEVELOPMENT.md` 已具备快速开始、插件描述、命令、事件、调度、玩法 API 和贡献规则，但还需要 Javadocs、教程化示例和迁移说明。

**产出**: 完整的 API 文档和参考实现，可以编写简单的测试插件

**下一步**: 收敛 Phase 1 的稳定性、文档和生态入口：补齐 Javadocs、官方示例插件、插件失败路径测试、端到端服务器启动与插件加载验证，并把 MiniMOTD 这类真实移植案例整理成外部开发者能照着做的文档。

---

### Phase 2: 性能优化实施 (6-10 周) 🚧 **进行中**

**目标**: 实现关键性能优化

- [x] **异步区块系统（第一轮）**
  - 专用 chunk load/worldgen executor
  - FlowSched 风格 chunk step 调度探索
  - 玩家移动/传送方向预加载
  - spawn chunk 等待卡住的 watchdog/重调度诊断与修复
  - 未完成：长期 soak、极端爆炸/同步读、future 卡住路径继续收敛

- [x] **网络层优化（第一轮）**
  - 异步 chunk packet preparation
  - prepared chunk packet frames
  - packet flush / outbound queue coalescing
  - status pong flush 修复
  - 未完成：异地弱网、丢包、高 RTT 下的真实体验 benchmark

- [ ] **实体系统优化**
  - 已实现部分碰撞、实体 section scan、AI target/goal/sensor fast path
  - 未完成：完整 entity activation range、AI 预算、密集实体压力测试

- [ ] **内存和数据路径优化**
  - 已实现部分随机 tick、流体、worldgen、explosion、packet、item merge 等热点分配优化
  - 未完成：NBT 序列化、内存泄漏检测、统一 profile/benchmark 报告

**产出**: 性能测试报告，与 Paper 的对比基准测试

---

### Phase 3: 功能增强 (8-12 周)

**目标**: 实现独特的功能增强

- [ ] **高级红石系统（暂停）**
  - 红石 JIT / profiler / shadow / wire hot path 已拆除
  - 近期不再推进红石 JIT，优先保证原版红石顺序、生电机器兼容和技术服兼容开关稳定
  - 未来如重启红石优化，必须先建立固定红石兼容测试集，再从纯观测工具开始

- [ ] **自定义实体框架**
  - 插件可注册完全自定义的实体
  - 自定义 AI 和行为树
  - 客户端资源包集成

- [ ] **数据包增强**
  - 更强大的数据包 API
  - 运行时数据包重载
  - 跨维度数据包

- [ ] **调试和监控工具**
  - 基础 TPS/MSPT 采样、命令和 GUI 显示 ✅
  - Fand watchdog 已替换原版 watchdog，支持更详细但限流的诊断日志 ✅
  - 内置性能分析工具
  - 区块和实体可视化

**产出**: 功能演示和使用文档

---

### Phase 4: 稳定性和生态 (持续进行)

**目标**: 打磨产品，构建社区

- [ ] **全面测试**
  - 单元测试覆盖率 >70%
  - 集成测试套件
  - 压力测试和崩溃恢复测试

- [ ] **文档完善**
  - 插件开发教程
  - API 参考文档
  - 最佳实践指南
  - 从 Bukkit 迁移指南

- [x] **示例插件（测试插件阶段）**
  - 测试插件已覆盖命令、事件、GUI、物品组件、配方、粒子、声音、BossBar、Title、ActionBar、Scoreboard、Tab 等参考用法 ✅
  - MiniMOTD Fand 平台移植已完成探索，可作为真实插件移植案例 ✅
  - 独立官方示例插件库（待实现）
  - 常见功能参考实现（持续补充）

- [ ] **社区建设**
  - Discord 服务器
  - GitHub Discussions
  - 贡献者招募

**产出**: 生产就绪的第一个稳定版本（1.0.0）

---

## 技术挑战与风险

### 挑战 1: Mojang 代码的法律问题

**风险**: Mojang EULA 限制

**应对**:
- 不直接分发反混淆后的代码
- 只分发 patch 文件
- 用户需要自己运行构建工具
- 参考 Paper 的法律处理方式

### 挑战 2: 版本更新维护

**风险**: 每次 Minecraft 更新都需要重新适配

**应对**:
- 自动化 patch 应用和冲突检测
- 模块化设计，减少对原版代码的侵入
- 维护详细的修改文档

### 挑战 3: 生态冷启动

**风险**: 没有现成的插件，开发者不愿意迁移

**应对**:
- 提供优秀的文档和示例
- 开发常用功能的官方插件
- 提供 Bukkit API 兼容层（可选模块）
- 突出性能优势吸引技术型用户

### 挑战 4: 性能验证

**风险**: 宣称的性能优势无法兑现

**应对**:
- 建立完善的基准测试框架
- 定期发布性能对比报告
- 公开测试方法和数据
- 接受社区的性能挑战和反馈

---

## 成功指标

### 短期目标 (6 个月)

- [x] 能够运行原版世界，玩家可以正常游玩
- [x] 基础启动、玩家登录、插件命令和演示流程已验证
- [ ] 至少 5 个功能性插件（官方开发或官方维护移植）
- [x] 性能测试中至少一个指标超越 Paper/原版路径（区块生成与网络发送已有阶段性结果，仍需公开 benchmark）
- [ ] 10+ 外部贡献者参与

### 中期目标 (12 个月)

- [ ] 50+ 社区开发的插件
- [ ] 100+ 生产环境服务器使用
- [ ] 主要性能指标全面超越 Paper
- [ ] 稳定的月度发布节奏

### 长期目标 (24 个月)

- [ ] 成为 Minecraft 服务端的主流选择之一
- [ ] 500+ 插件生态
- [ ] 1000+ 活跃用户
- [ ] 被大型服务器网络采用

---

## 资源需求

### 开发者时间

- **全职投入**: 40-50 小时/周
- **预计时长**: 6-12 个月达到 MVP
- **长期维护**: 每周 10-20 小时

### 硬件需求

- **开发机**: 16GB+ RAM, 多核 CPU
- **测试服务器**: 云服务器或专用机器进行压力测试
- **CI/CD**: GitHub Actions（免费额度应该够用）

### 外部依赖

- **文档托管**: GitHub Pages
- **社区平台**: Discord
- **问题追踪**: GitHub Issues
- **代码托管**: GitHub

---

## 下一步行动

### 已完成的初始行动

1. ✅ 完成本企划书
2. ✅ 创建 GitHub 仓库，设置基础结构
3. ✅ 研究 Paper 的 paperweight 构建系统（已采用）
4. ✅ 设计项目模块结构（API/Server/Clip）

### 当前近期计划（2026-07）

1. ✅ 实现基础的 patch 应用系统
2. ✅ 成功编译并运行原版服务端
3. ✅ 制作第一个简单的 patch（服务器启动日志）
4. [ ] 编写 CONTRIBUTING.md
5. ✅ 实现 EventBus 和 PluginManager 的基础逻辑
6. ✅ 将 FandServer 与原版 MinecraftServer 集成
7. ✅ 实现 TPS/MSPT 采样、命令和 GUI 展示基础能力
8. ✅ 实现 Component/DataComponent、Recipe、Particle/Sound、Scoreboard/Tab 等核心 API 示例路径
9. ✅ 接入 vanilla registry key 数据生成模块
10. [x] 完成命令 API 第一轮重构（Builder + 注解命令）
11. [x] 完成 Fand watchdog 第一轮替换
12. [x] 完成区块生成、chunk packet、登录 spawn 等性能/稳定性优化第一轮
13. [x] 完成 MiniMOTD Fand 平台移植探索
14. [ ] 为高频事件、插件失败路径、区块系统、网络发送和构建内存占用建立回归测试/基准
15. [ ] 删除已暂停的红石 JIT 路线，回到原版红石执行路径

### 下一阶段目标（1-2 个月）

1. [ ] 完成 Phase 0 的所有任务（CI 集成测试、压力测试和发布矩阵）
2. [ ] 完成 Phase 1 的稳定性硬化、Javadocs 和 API 文档
3. ✅ 编写第一个可工作的测试插件
4. [ ] 整理测试插件为可读的官方示例入口，并拆出独立示例插件库
5. [ ] 准备 5-15 个官方/移植插件案例，支撑 Modrinth 平台申请
6. [ ] 发布第一个 Pre-Alpha 版本
7. [ ] 建立公开 benchmark 页面，说明测试方法、硬件、参数和数据

---

## 项目命名与品牌

**Fand** - 简洁、易记、独特

**Slogan 候选**:
- "Performance Reimagined"（性能重塑）
- "Modern Server, Classic Game"（现代服务端，经典游戏）
- "Built for Scale"（为规模而生）

**Logo**: 待设计（可考虑与 Minecraft 风格相关但有区别的几何图形）

---

## 结论

Fand Server 是一个雄心勃勃但可行的项目。通过基于原版代码进行增强而非完全重写，我们可以：

- ✅ 确保与原版机制的完全兼容性
- ✅ 减少重复造轮子的工作量
- ✅ 专注于真正有价值的优化和功能
- ✅ 参考成熟项目（Paper）的成功经验

关键成功因素：

1. **渐进式开发** - 不追求一步到位，而是持续迭代
2. **性能可验证** - 用数据说话，不做空洞宣传
3. **开发者体验优先** - API 设计是核心竞争力
4. **社区驱动** - 尽早开源，吸引贡献者

---

**项目开始日期**: 2026年6月3日  
**预计首个可用版本**: 2026年9月  
**预计 1.0 稳定版**: 2027年1月

---

## 附录

### 参考项目

- **CraftBukkit/Spigot**: 最成功的原版增强服务端
- **Paper**: 高性能的 Spigot 分支
- **Purpur**: Paper 的功能增强分支
- **Folia**: Paper 的多线程实验分支
- **Minestom**: 从零重写的现代服务端（参考架构设计）

### 学习资源

- [wiki.vg](https://wiki.vg/) - Minecraft 协议文档
- [Paper 开发者文档](https://docs.papermc.io/)
- [Minecraft 源码分析](https://fabricmc.net/wiki/)

### 法律声明

Fand Server 是独立的第三方项目，与 Mojang Studios 和 Microsoft 无关。所有 Minecraft 相关的商标和版权归其合法所有者。本项目遵守 Minecraft 最终用户许可协议。
