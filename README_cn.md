Fand
===========

[![Fand CI](https://github.com/FandMC/Fand/actions/workflows/build.yml/badge.svg)](https://github.com/FandMC/Fand/actions/workflows/leaves.yml)
[![Fand Download](https://img.shields.io/github/downloads/FandMC/Fand/total?color=0&logo=github)](https://github.com/FandMC/Fand/releases/latest)
[![QQ](https://img.shields.io/badge/QQ_Unofficial-495796642-blue)](https://qm.qq.com/q/LGP8GVm9SE)

[English](README.md) | **中文**

> 一个致力于修复原版服务端被破坏特性的 [Paper](https://github.com/PaperMC/Paper) 分支

> 你可以在 [这里](https://docs.fandmc.cn/zh_Hans/fand/reference/configuration) 查看所有的修改和修复内容

## 对于服务器管理员
此分支使用与 Paper 一致的 fandclip(paperclip的分支) 分发

你可以从 [此处](https://github.com/FandMC/Fand/releases/latest) 下载最新的构建结果 (1.21.x)

也可以通过 [此处](#自行构建) 的指南自行构建

如果你想要获得更多信息，那么你可以访问我们的 [文档](https://docs.fandmc.cn/zh_Hans/fand/guides/getting-started)

## 对于插件开发者
Fand-API:
```kotlin
maven {
    name = "leavesmc-repo"
    url = "https://repo.fandmc.cn/snapshots/"
}

dependencies {
    compileOnly("com.fandmc.fand:fand-api:1.21.3-R0.1-SNAPSHOT")
}
 ```

如果你要将 Fand 作为依赖,那么你必须进行 [自行构建](#自行构建)

Fand-Server:
```kotlin
dependencies {
    compileOnly("com.fandmc.fand:fand:1.21.3-R0.1-SNAPSHOT")
}
 ```

## 自行构建

你需要最低 JDK 21 和一个可以正常访问各种 git/maven 库的网络

首先克隆此储存库，然后在你的终端里依次执行 `./gradlew applyPatches` 和 `./gradlew createMojmapFandclipJar`

最后 你可以在 `build/libs` 文件夹里找到对应的jar文件

## 对于想要出一份力的开发者

可查看 [贡献须知](docs/CONTRIBUTING_cn.md)
