plugins {
    `java-library`
    id("io.papermc.paperweight.core")
}

description = "Fand Server core implementation"

dependencies {
    api(project(":fand-api"))

    mache("io.papermc:mache:26.1.2+build.1")
    paperclip("io.papermc:paperclip:3.0.4")

    compileOnly("ch.qos.logback:logback-classic:1.5.6")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("it.unimi.dsi:fastutil:8.5.13")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.mockito:mockito-core:5.12.0")
}

paperweight {
    minecraftVersion.set(providers.gradleProperty("mcVersion"))
    gitFilePatches.set(false)
}

val generateBuildInfo by tasks.registering(WriteProperties::class) {
    destinationFile.set(layout.buildDirectory.file("generated/resources/fand-build.properties"))
    property("version", project.version.toString())
    property("minecraftVersion", providers.gradleProperty("minecraftVersion").get())
}

sourceSets.named("main") {
    resources.srcDir(generateBuildInfo.map { it.destinationFile.get().asFile.parentFile })
}

tasks.named("processResources") {
    dependsOn(generateBuildInfo)
}

val fatJar by tasks.registering(Jar::class) {
    dependsOn(project(":fand-api").tasks.named("classes"))

    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes("Main-Class" to "io.fand.server.Main")
    }

    from(sourceSets.main.get().output)
    from(project(":fand-api").sourceSets.main.get().output)

    from(configurations.runtimeClasspath.get().filter {
        it.name.endsWith(".jar") && !it.name.contains("mache") && !it.name.startsWith("fand-api-")
    }.map { if (it.isDirectory) it else zipTree(it) })

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    exclude("META-INF/versions/*/module-info.class")
    exclude("**/module-info.class")

    // Exclude Logback to use Minecraft's Log4j2
    exclude("logback*.xml")
    exclude("ch/qos/logback/**")
}

tasks.jar {
    archiveClassifier.set("slim")
}

tasks.assemble {
    dependsOn(fatJar)
}
