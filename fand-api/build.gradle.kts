plugins {
    `java-library`
}

description = "Fand Server plugin API"

dependencies {
    api("net.kyori:adventure-api:4.17.0")
    api("net.kyori:adventure-text-serializer-gson:4.17.0")
    api("org.jetbrains:annotations:24.1.0")
    api("org.slf4j:slf4j-api:2.0.13")

    compileOnlyApi("org.jspecify:jspecify:1.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
}
