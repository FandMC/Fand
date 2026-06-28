rootProject.name = "fand"

pluginManagement {
    repositories {
        maven("https://repo.fandmc.cn/repository/maven-public/")
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

include(
    "fand-data-generator",
    "fand-api",
    "fand-server",
)

dependencyResolutionManagement {
    repositories {
        maven("https://repo.fandmc.cn/repository/maven-public/")
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.neoforged.net/releases/")
    }
}
