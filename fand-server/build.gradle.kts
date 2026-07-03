import org.apache.logging.log4j.core.config.plugins.processor.PluginCache
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import java.util.Vector
import java.util.zip.ZipFile
import groovy.json.JsonSlurper

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
    `maven-publish`
    id("io.papermc.paperweight.core")
}

description = "Fand Server core implementation"

val fandclipVersion = providers.gradleProperty("fandclipVersion").orElse("latest.release").get()
val fandclipLauncher by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    api(project(":fand-api"))
    fandclipLauncher("io.fand:fandclip:$fandclipVersion")

    mache("io.papermc:mache:26.2-rc-2+build.2")
    paperclip("io.papermc:paperclip:3.0.4")

    compileOnly("ch.qos.logback:logback-classic:1.5.6")
    compileOnly("org.apache.logging.log4j:log4j-core:2.25.2")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.yaml:snakeyaml:2.4")
    implementation("com.electronwill.night-config:toml:3.8.1")
    implementation("it.unimi.dsi:fastutil:8.5.13")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("net.minecrell:terminalconsoleappender:1.3.0")
    implementation("org.jline:jline-terminal:3.27.0")
    implementation("org.jline:jline-terminal-jni:3.27.0")
    implementation("org.jline:jline-reader:3.27.0")
    runtimeOnly("net.kyori:option:1.1.0")

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

val generatedConsoleLanguageResources = layout.buildDirectory.dir("generated/console-language-resources")
val syncConsoleLanguages by tasks.registering {
    group = "fand"
    description = "Downloads bundled Minecraft language files used by the Fand console."

    val minecraftVersion = providers.gradleProperty("mcVersion")
    val outputDir = generatedConsoleLanguageResources

    inputs.property("minecraftVersion", minecraftVersion)
    outputs.dir(outputDir)

    doLast {
        val version = minecraftVersion.get()
        val manifest = JsonSlurper().parse(
            uri("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json").toURL()
        ) as Map<*, *>
        val versionEntry = (manifest["versions"] as List<*>)
            .filterIsInstance<Map<*, *>>()
            .firstOrNull { it["id"] == version }
            ?: error("Minecraft version '$version' was not found in Mojang version manifest")
        val versionMetadata = JsonSlurper().parse(
            uri(versionEntry["url"].toString()).toURL()
        ) as Map<*, *>
        val assetIndex = versionMetadata["assetIndex"] as Map<*, *>
        val assetIndexMetadata = JsonSlurper().parse(
            uri(assetIndex["url"].toString()).toURL()
        ) as Map<*, *>
        val objects = assetIndexMetadata["objects"] as Map<*, *>

        val languagePath = "minecraft/lang/zh_cn.json"
        val languageObject = objects[languagePath] as? Map<*, *>
            ?: error("Asset index for Minecraft $version does not contain $languagePath")
        val hash = languageObject["hash"].toString()
        val url = "https://resources.download.minecraft.net/${hash.substring(0, 2)}/$hash"

        val output = outputDir.get().asFile.toPath()
            .resolve("assets/minecraft/lang/zh_cn.json")
        Files.createDirectories(output.parent)
        uri(url).toURL().openStream().use { input ->
            Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

sourceSets.named("main") {
    resources.srcDir(generateBuildInfo.map { it.destinationFile.get().asFile.parentFile })
    resources.srcDir(generatedConsoleLanguageResources)
}

tasks.named("processResources") {
    dependsOn(generateBuildInfo)
    dependsOn(syncConsoleLanguages)
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(syncConsoleLanguages)
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

val bundledFandclipLayoutRoot = layout.buildDirectory.dir("generated/fandclip-layout")
val bundledFandclipManifestFile = layout.buildDirectory.file("generated/fandclip-manifest/clip-manifest.properties")
val serverRuntimeClasspath = configurations.named("runtimeClasspath")

val prepareBundledFandclipLayout by tasks.registering {
    val serverJar = tasks.named<Jar>("jar")

    dependsOn(serverJar)
    dependsOn(serverRuntimeClasspath)

    inputs.file(serverJar.flatMap { it.archiveFile })
    inputs.files(serverRuntimeClasspath)
    outputs.dir(bundledFandclipLayoutRoot)
    outputs.file(bundledFandclipManifestFile)

    doLast {
        val root = bundledFandclipLayoutRoot.get().asFile.toPath()
        project.delete(root)

        val serverVersionId = "fand-${project.version}"
        val versionPath = "$serverVersionId/fand-server-$serverVersionId.jar"
        val versionFile = root.resolve("META-INF/versions").resolve(versionPath)
        Files.createDirectories(versionFile.parent)
        Files.copy(serverJar.get().archiveFile.get().asFile.toPath(), versionFile)

        val versionsList = listOf("${sha256(versionFile)}\t$serverVersionId\t$versionPath")

        val librariesRoot = root.resolve("META-INF/libraries")
        val libraries = serverRuntimeClasspath.get()
            .incoming
            .artifacts
            .artifacts
            .asSequence()
            .filter { artifact -> artifact.file.isFile && artifact.file.name.endsWith(".jar") && !artifact.file.name.contains("mache") }
            .map { artifact ->
                val id = artifactId(artifact.id.componentIdentifier)
                val path = artifactPath(artifact.id.componentIdentifier, artifact.file.name)
                val output = librariesRoot.resolve(path)
                Files.createDirectories(output.parent)
                Files.copy(artifact.file.toPath(), output, StandardCopyOption.REPLACE_EXISTING)
                "${sha256(output)}\t$id\t$path"
            }
            .distinct()
            .sorted()
            .toList()

        val metaInf = root.resolve("META-INF")
        Files.createDirectories(metaInf)
        Files.writeString(metaInf.resolve("versions.list"), versionsList.joinToString(separator = "\n", postfix = "\n"))
        Files.writeString(metaInf.resolve("libraries.list"), libraries.joinToString(separator = "\n", postfix = "\n"))
        Files.writeString(metaInf.resolve("main-class"), "io.fand.server.Main")

        val manifestFile = bundledFandclipManifestFile.get().asFile.toPath()
        Files.createDirectories(manifestFile.parent)
        Files.writeString(
            manifestFile,
            "fandVersion=${project.version}\nminecraftVersion=${providers.gradleProperty("minecraftVersion").get()}\n",
        )
    }
}

val fandclipJar by tasks.registering(Jar::class) {
    group = BasePlugin.BUILD_GROUP
    description = "Assembles a complete runnable Fandclip jar containing this Fand server build."

    dependsOn(fandclipLauncher)
    dependsOn(prepareBundledFandclipLayout)

    archiveBaseName.set("fand-server")
    archiveClassifier.set("clip")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes("Main-Class" to "io.fand.fandclip.Fandclip")
    }

    from({ zipTree(fandclipLauncher.singleFile) }) {
        exclude(
            "META-INF/MANIFEST.MF",
            "META-INF/libraries/**",
            "META-INF/versions/**",
            "META-INF/libraries.list",
            "META-INF/versions.list",
            "META-INF/main-class",
            "clip-manifest.properties",
        )
    }
    from(prepareBundledFandclipLayout)
}

tasks.assemble {
    dependsOn(fandclipJar)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(fandclipJar)

            pom {
                name.set("Fand Server")
                description.set(project.description)
            }
        }
    }

    repositories {
        maven {
            name = "fandLocal"
            val snapshot = project.version.toString().endsWith("-SNAPSHOT")
            val defaultUrl = if (snapshot) {
                "https://repo.fandmc.cn/repository/maven-snapshots/"
            } else {
                "https://repo.fandmc.cn/repository/maven-releases/"
            }
            url = uri(
                providers.gradleProperty("fandRepoUrl")
                    .orElse(providers.environmentVariable("FAND_REPO_URL"))
                    .orElse(defaultUrl)
                    .get()
            )
            credentials {
                username = providers.gradleProperty("fandRepoUser")
                    .orElse(providers.environmentVariable("FAND_REPO_USER"))
                    .orElse("")
                    .get()
                password = providers.gradleProperty("fandRepoPassword")
                    .orElse(providers.environmentVariable("FAND_REPO_PASSWORD"))
                    .orElse("")
                    .get()
            }
        }
    }
}

fun artifactId(identifier: ComponentIdentifier): String {
    return when (identifier) {
        is ModuleComponentIdentifier -> "${identifier.group}:${identifier.module}:${identifier.version}"
        is ProjectComponentIdentifier -> {
            val dependency = project.findProject(identifier.projectPath)
            "${dependency?.group ?: project.group}:${dependency?.name ?: identifier.projectName}:${dependency?.version ?: project.version}"
        }
        else -> identifier.displayName
    }
}

fun artifactPath(identifier: ComponentIdentifier, fileName: String): String {
    return when (identifier) {
        is ModuleComponentIdentifier -> "${identifier.group.replace('.', '/')}/${identifier.module}/${identifier.version}/$fileName"
        is ProjectComponentIdentifier -> {
            val dependency = project.findProject(identifier.projectPath)
            val group = (dependency?.group ?: project.group).toString().replace('.', '/')
            val name = dependency?.name ?: identifier.projectName
            val version = dependency?.version ?: project.version
            "$group/$name/$version/$fileName"
        }
        else -> "unknown/${sanitizePathSegment(identifier.displayName)}/$fileName"
    }
}

fun sanitizePathSegment(value: String): String {
    return value.replace(Regex("[^A-Za-z0-9._-]"), "_")
}

fun sha256(path: java.nio.file.Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) {
                break
            }
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
