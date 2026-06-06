plugins {
    `java-library`
}

description = "Sample plugin used for end-to-end runtime smoke testing"

dependencies {
    compileOnly(project(":fand-api"))

    testImplementation(project(":fand-api"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
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
