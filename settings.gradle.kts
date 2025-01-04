pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.fandmc.cn/snapshots/")
        maven("https://repo.leavesmc.org/snapshots/")
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

rootProject.name = "Fand"

include("fand-api", "fand-server", "paper-api-generator")
