plugins {
    id("io.fand.plugin") version "latest.release"
}

description = "Minimal Fand API example plugin"

fandPlugin {
    mainClass.set("io.fand.testplugin.TestPlugin")
    directRunGuard.set(true)
}

dependencies {
    compileOnly(files("../fand-api/build/classes/java/main"))
    compileOnly(files("../fand-api/build/generated/sources/fandData/main/java"))
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("fand-test-plugin")
    archiveClassifier.set("")
    manifest {
        attributes(
            "Implementation-Title" to "Fand Test Plugin",
            "Implementation-Version" to project.version,
        )
    }
}
