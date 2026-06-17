import org.apache.logging.log4j.core.config.plugins.processor.PluginCache
import java.io.File
import java.net.URL
import java.util.Vector
import java.util.zip.ZipFile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.apache.logging.log4j:log4j-core:2.25.2")
    }
}

plugins {
    `java-library`
    id("io.papermc.paperweight.core")
}

description = "Fand Server core implementation"

dependencies {
    api(project(":fand-api"))

    mache("io.papermc:mache:26.2-rc-2+build.2")
    paperclip("io.papermc:paperclip:3.0.4")

    compileOnly("ch.qos.logback:logback-classic:1.5.6")
    compileOnly("org.apache.logging.log4j:log4j-core:2.25.2")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.yaml:snakeyaml:2.4")
    implementation("com.electronwill.night-config:toml:3.8.1")
    implementation("it.unimi.dsi:fastutil:8.5.13")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("com.viaversion:viaversion-common:5.9.1")
    implementation("net.minecrell:terminalconsoleappender:1.3.0")
    implementation("org.jline:jline-terminal:3.27.0")
    implementation("org.jline:jline-terminal-jni:3.27.0")
    implementation("org.jline:jline-reader:3.27.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.mockito:mockito-core:5.12.0")
}

val log4jPlugins = sourceSets.create("log4jPlugins")
configurations.named(log4jPlugins.compileClasspathConfigurationName) {
    extendsFrom(configurations.compileClasspath.get())
}

val integrationTestSourceSet = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

val benchmarkSourceSet = sourceSets.create("benchmark") {
    compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

configurations.named(integrationTestSourceSet.implementationConfigurationName) {
    extendsFrom(configurations.testImplementation.get())
}
configurations.named(integrationTestSourceSet.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.testRuntimeOnly.get())
}
configurations.named(benchmarkSourceSet.implementationConfigurationName) {
    extendsFrom(configurations.testImplementation.get())
}
configurations.named(benchmarkSourceSet.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.testRuntimeOnly.get())
}

dependencies {
    log4jPlugins.annotationProcessorConfigurationName("org.apache.logging.log4j:log4j-core:2.25.2")
    "log4jPluginsCompileOnly"("org.apache.logging.log4j:log4j-core:2.25.2")
}

tasks.named<JavaCompile>(log4jPlugins.compileJavaTaskName) {
    options.compilerArgs.addAll(
        listOf(
            "-Alog4j.graalvm.groupId=${project.group}",
            "-Alog4j.graalvm.artifactId=${project.name}",
        )
    )
}

val integrationTest by tasks.registering(Test::class) {
    description = "Runs Fand server integration tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    shouldRunAfter(tasks.test)
    useJUnitPlatform()
}

val benchmark by tasks.registering(JavaExec::class) {
    description = "Runs lightweight Fand server microbenchmarks."
    group = "verification"
    mainClass.set("io.fand.server.benchmark.FandBenchmarkRunner")
    classpath = benchmarkSourceSet.runtimeClasspath
}

tasks.check {
    dependsOn(integrationTest)
}

paperweight {
    minecraftVersion.set(providers.gradleProperty("mcVersion"))
    gitFilePatches.set(false)

    paper {
        sourcePatchDir.set(layout.projectDirectory.dir("patches/sources"))
        resourcePatchDir.set(layout.projectDirectory.dir("patches/resources"))
        featurePatchDir.set(layout.projectDirectory.dir("patches/features"))
    }
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

val log4jPluginsCachePath = "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat"
val log4jPluginsRoot = layout.buildDirectory.dir("generated/log4j-plugins")

val mergeLog4jPlugins by tasks.registering {
    dependsOn(log4jPlugins.classesTaskName)
    val inputJars = configurations.runtimeClasspath.map { cfg ->
        cfg.filter { it.name.endsWith(".jar") && !it.name.contains("mache") }
    }
    val log4jPluginsClasses = log4jPlugins.output.classesDirs
    val outputFile = log4jPluginsRoot.map { it.file(log4jPluginsCachePath) }
    inputs.files(inputJars).withPropertyName("inputJars")
    inputs.files(log4jPluginsClasses).withPropertyName("log4jPluginsClasses")
    outputs.file(outputFile).withPropertyName("outputFile")
    doLast {
        val urls = Vector<URL>()
        inputJars.get().forEach { jar ->
            ZipFile(jar).use { zip ->
                val entry = zip.getEntry(log4jPluginsCachePath) ?: return@use
                val temp = File.createTempFile("log4j-dat-", ".dat").also { it.deleteOnExit() }
                zip.getInputStream(entry).use { input -> temp.outputStream().use { input.copyTo(it) } }
                urls.add(temp.toURI().toURL())
            }
        }
        log4jPluginsClasses.forEach { dir ->
            val datFile = File(dir, log4jPluginsCachePath)
            if (datFile.isFile) urls.add(datFile.toURI().toURL())
        }
        val cache = PluginCache()
        cache.loadCacheFiles(urls.elements())
        val out = outputFile.get().asFile
        out.parentFile.mkdirs()
        out.outputStream().use { cache.writeCache(it) }
        logger.lifecycle("Merged ${urls.size} Log4j2Plugins.dat caches → ${cache.size()} entries")
    }
}

tasks.jar {
    dependsOn(mergeLog4jPlugins)

    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes("Main-Class" to "io.fand.server.Main")
    }

    from(log4jPluginsRoot)
    from(sourceSets.main.get().output)
    from(log4jPlugins.output) {
        exclude(log4jPluginsCachePath)
    }

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    exclude("META-INF/versions/*/module-info.class")
    exclude("**/module-info.class")
}
