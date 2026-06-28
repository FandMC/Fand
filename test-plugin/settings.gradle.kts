pluginManagement {
    repositories {
        maven("https://repo.fandmc.cn/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://repo.fandmc.cn/repository/maven-public/")
        mavenCentral()
    }
}

rootProject.name = "fand-test-plugin"
