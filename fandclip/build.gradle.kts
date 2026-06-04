plugins {
    `java-library`
    application
}

description = "Fandclip launcher: downloads vanilla libraries on first run and boots the Fand server."

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

application {
    mainClass.set("io.fand.fandclip.Fandclip")
    applicationName = "fandclip"
}

val generateClipManifest by tasks.registering(WriteProperties::class) {
    destinationFile.set(layout.buildDirectory.file("generated/resources/clip-manifest.properties"))
    property("fandVersion", project.version.toString())
    property("minecraftVersion", providers.gradleProperty("minecraftVersion").get())
}

val embedServerJar by tasks.registering(Copy::class) {
    dependsOn(":fand-server:fatJar")
    from(project(":fand-server").tasks.named<Jar>("fatJar").map { it.archiveFile })
    rename { "fand-server.jar" }
    into(layout.buildDirectory.dir("generated/resources"))
}

sourceSets.named("main") {
    resources.srcDir(layout.buildDirectory.dir("generated/resources"))
}

tasks.named("processResources") {
    dependsOn(generateClipManifest, embedServerJar)
}

tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.WARN
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Main-Class" to "io.fand.fandclip.Fandclip",
            "Multi-Release" to "true"
        )
    }
}
