import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    id("org.jetbrains.intellij") version "0.4.16"
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("java")
    signing
    `maven-publish`
    id("com.jfrog.bintray")

    id("com.github.johnrengelman.shadow")
}

repositories {
    maven("http://maven.aliyun.com/nexus/content/groups/public/")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    api(project(":kotlin-jvm-blocking-bridge"))
    api(project(":kotlin-jvm-blocking-bridge-compiler"))

    compileOnly("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}")
    compileOnly(files("libs/ide-common.jar"))
}

version = Versions.idePlugin

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2020.3.1"
    isDownloadSources = true
    updateSinceUntilBuild = false

    setPlugins(
        "org.jetbrains.kotlin:203-1.4.30-RC-IJ6682.9@eap"
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
    untilBuild("215.*")
    changeNotes("""
        See <a href="">Release notes</a>
    """.trimIndent())
    setPluginDescription("""
        Provides @JvmBlockingBridge calls for Java. <br/>  
        Find more information on source repository: <a href="https://github.com/him188/kotlin-jvm-blocking-bridge">kotlin-jvm-blocking-bridge</a>.
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