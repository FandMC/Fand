# Fand

[English](README.md) | [简体中文](README.zh-CN.md)

一个为 Java Edition 26.2 打造的、经过补丁修改的原版 Minecraft 服务端，
拥有全新的、类型安全的插件 API。在精神上是 Paper 的孪生兄弟：相同的工作机制（基于反编译的原版服务端，以补丁链形式交付），但呈现不同的外观（不兼容 Bukkit，拥有现代化的核心 API）。

**当前状态**: 阶段 0 已完成，阶段 1 处于后期。构建流水线可正常运行；运行时与经过补丁的 vanilla 代码的集成已在插件、事件、命令、调度、配置、物品栏和代理转发等方面全面生效。阶段 1 的剩余工作主要是加固、文档和端到端验证，而非初始运行环境的搭建。

## 模块

| 模块        | 作用                                                                 |
|---------------|----------------------------------------------------------------------|
| `fand-api`    | 公共插件 API。提供稳定的表面，不含任何实现细节。        |
| `fand-server` | 服务端运行时。通过 paperweight 承载经过补丁的 vanilla 代码。      |
| `fand-server/patches/` | 规范化的 paperweight 补丁集，应用于 vanilla 代码之上。 |

Fandclip 已作为独立项目维护；`fand-server` 在组装可运行 clip jar 时通过 Maven 获取它。

## 构建管线

我们使用 **paperweight-core** 来管理工作流。

```
piston-meta → paperweight → vanilla-bundler.jar
            → unbundleServer → vanilla-server.jar
            → decompileServer → decompiled/ (通过 Mache 补丁)
            → applyPatches → fand-server/src/minecraft/java (从 fand-server/patches/ 通过 git)
edit sources → rebuildPatches → fand-server/patches/ (更新)
```

Paperweight 任务会在 `:fand-server` 子项目中自动可用。经过补丁的 Minecraft 代码在完成设置后位于`fand-server/src/minecraft/java`。

## 当前状态

**阶段 0 (构建基础设施)**: ✅ 完成
- Paperweight 集成可用
- 补丁系统可运行
- 模块结构已建立
- 首个示例补丁已应用

**阶段 1 (核心 API 设计)**: 🚧 进行中
- API 接口已定义 (事件、插件、调度器、命令、权限、世界/实体、物品栏/物品)
- 插件、事件、调度器、命令、权限、配置重载、物品栏/物品以及代理转发的运行时实现已存在
- 通过有序的功能补丁正在进行 vanilla 集成

**当前的工程重点**:
- 加固插件故障路径和资源清理
- 减少热点事件补丁中的内存分配和重复运行时查找
- 在公共 API 文档中阐明调度器的线程/刻语义
- 将集成测试从当前的 Gradle 源代码集扩展到完整的服务端启动、插件加载和关闭验证
- 稳定 paperweight 工作流的本地/CI 构建内存设置

完整路线图详见 `PROJECT_PROPOSAL.md`，开发指南详见 `CODING_STANDARDS.md`。

## 许可证

Fand 使用 GNU General Public License v3.0 授权。完整许可证文本请参阅 `LICENSE`。
