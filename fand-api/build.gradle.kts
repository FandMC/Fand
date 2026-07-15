plugins {
    `java-library`
    `maven-publish`
}

description = "Fand Server plugin API"

val generatedFandDataSources = layout.buildDirectory.dir("generated/sources/fandData/main/java")
val minecraftSourceRoot = rootProject.layout.projectDirectory.dir("fand-server/src/minecraft/java")
val apiSourceRoot = layout.projectDirectory.dir("src/main/java")
val fandDataGenerator by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val generateFandData by tasks.registering(JavaExec::class) {
    group = "fand"
    description = "Generates vanilla registry key API sources from the current Minecraft sources."
    dependsOn(":fand-data-generator:classes")

    classpath = fandDataGenerator
    mainClass.set("io.fand.datagenerator.FandDataGenerator")

    args(
        minecraftSourceRoot.asFile.absolutePath,
        generatedFandDataSources.get().asFile.absolutePath,
        apiSourceRoot.asFile.absolutePath,
    )
    inputs.dir(minecraftSourceRoot)
    inputs.dir(apiSourceRoot)
    inputs.files(fandDataGenerator).withPropertyName("generatorClasspath")
    outputs.dir(generatedFandDataSources)
}

sourceSets.named("main") {
    java.srcDir(generatedFandDataSources)
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateFandData)
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(generateFandData)
}

tasks.named<Javadoc>("javadoc") {
    dependsOn(generateFandData)
}

dependencies {
    fandDataGenerator(project(":fand-data-generator"))

    api("com.google.code.gson:gson:2.11.0")
    api("net.kyori:adventure-api:4.26.1")
    api("net.kyori:adventure-text-minimessage:4.26.1")
    api("net.kyori:adventure-text-serializer-gson:4.26.1")
    api("org.jetbrains:annotations:24.1.0")
    api("org.slf4j:slf4j-api:2.0.13")

    compileOnlyApi("org.jspecify:jspecify:1.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("Fand API")
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
