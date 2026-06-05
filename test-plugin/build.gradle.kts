plugins {
    `java-library`
}

description = "Sample plugin used for end-to-end runtime smoke testing"

dependencies {
    compileOnly(project(":fand-api"))
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
