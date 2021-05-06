import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    id("io.github.karlatemp.publication-sign")
    id("org.jetbrains.intellij") version "0.7.3"
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("java")
    signing
    `maven-publish`

    id("com.github.johnrengelman.shadow")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    api(project(":kotlin-jvm-blocking-bridge"))
    api(project(":kotlin-jvm-blocking-bridge-compiler"))
    api(project(":kotlin-jvm-blocking-bridge-gradle"))

    // compileOnly("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}")
    compileOnly(files("libs/ide-common.jar"))
}

version = Versions.idePlugin

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2021.1"
    isDownloadSources = true
    updateSinceUntilBuild = false

    sandboxDirectory = projectDir.resolve("run/idea-sandbox").absolutePath

    setPlugins(
        "org.jetbrains.kotlin:211-1.5.0-release-759-IJ6693.72", "java"
    )
}

tasks.getByName("publishPlugin", org.jetbrains.intellij.tasks.PublishTask::class) {
    val pluginKey = project.findProperty("jetbrains.hub.key")?.toString()
    if (pluginKey != null) {
        logger.info("Found jetbrains.hub.key")
        setToken(pluginKey)
    } else {
        logger.info("jetbrains.hub.key not found")
    }
}

tasks.withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask> {
    sinceBuild("201.*") // Kotlin does not support 193 anymore
    untilBuild("225.*")
    changeNotes("""
        See <a href="">Release notes</a>
    """.trimIndent())
}

setupPublishing(
    groupId = "net.mamoe",
    artifactId = "kotlin-jvm-blocking-bridge-intellij"
)

tasks.withType(KotlinJvmCompile::class) {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
}

val theProject = project

tasks.getByName("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveClassifier.set("")
    this.dependencyFilter.exclude {
        it.name.contains("intellij", ignoreCase = true) || it.name.contains("idea", ignoreCase = true)
    }
    exclude {
        // exclude ComponentRegistrar which is for CLI compiler.
        it.name == "org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar" && !it.path.contains(theProject.path)
    }
}

tasks.buildPlugin.get().dependsOn(tasks.shadowJar.get())

/*
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}*/

/*
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}*/
