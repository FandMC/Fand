rootProject.name = "fand"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

include(
    "fand-data-generator",
    "fand-api",
    "fand-server",
    "fandclip",
    "test-plugin",
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://repo.viaversion.com/")
    }
}
