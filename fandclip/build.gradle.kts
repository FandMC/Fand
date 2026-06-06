import java.nio.file.Files
import java.security.MessageDigest
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier

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
    destinationFile.set(layout.buildDirectory.file("generated/manifest/clip-manifest.properties"))
    property("fandVersion", project.version.toString())
    property("minecraftVersion", providers.gradleProperty("minecraftVersion").get())
}

val serverJar = project(":fand-server").tasks.named<Jar>("jar")
val serverRuntimeClasspath = project(":fand-server").configurations.named("runtimeClasspath")
val bundledLayoutRoot = layout.buildDirectory.dir("generated/bundler-layout")
val serverVersionId = "fand-${project.version}"

val prepareBundlerLayout by tasks.registering {
    dependsOn(serverJar)
    dependsOn(serverRuntimeClasspath)

    inputs.file(serverJar.flatMap { it.archiveFile })
    inputs.files(serverRuntimeClasspath)
    outputs.dir(bundledLayoutRoot)

    doLast {
        val root = bundledLayoutRoot.get().asFile.toPath()
        project.delete(root)

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
            .filter { it.file.isFile && it.file.name.endsWith(".jar") && !it.file.name.contains("mache") }
            .map { artifact ->
                val id = artifactId(artifact.id.componentIdentifier)
                val path = artifactPath(artifact.id.componentIdentifier, artifact.file.name)
                val output = librariesRoot.resolve(path)
                Files.createDirectories(output.parent)
                Files.copy(artifact.file.toPath(), output)
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
    }
}

tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.WARN
    from(generateClipManifest)
    from(prepareBundlerLayout)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Main-Class" to "io.fand.fandclip.Fandclip")
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
