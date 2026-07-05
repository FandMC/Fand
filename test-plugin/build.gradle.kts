plugins {
    id("io.fand.plugin") version "latest.release"
}

description = "Sample plugin used for end-to-end runtime smoke testing"

fandPlugin {
    mainClass.set("io.fand.testplugin.TestPlugin")
    directRunGuard.set(true)
}

dependencies {
    compileOnly(files("../fand-api/build/classes/java/main"))
    compileOnly(files("../fand-api/build/generated/sources/fandData/main/java"))
    testCompileOnly(files("../fand-api/build/classes/java/main"))
    testCompileOnly(files("../fand-api/build/generated/sources/fandData/main/java"))
    testRuntimeOnly(files("../fand-api/build/classes/java/main"))
    testRuntimeOnly(files("../fand-api/build/generated/sources/fandData/main/java"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
