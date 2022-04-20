import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.targets

plugins {
    id("org.jetbrains.intellij") version "1.5.3"
    kotlin("jvm")
    kotlin("plugin.serialization")

//    id("com.github.johnrengelman.shadow")
}

kotlin.targets.asSequence()
    .flatMap { it.compilations }
    .filter { it.platformType == org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm }
    .map { it.kotlinOptions }
    .filterIsInstance<org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions>()
    .forEach { it.jvmTarget = "11" }

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    api(project(":kotlin-jvm-blocking-bridge-runtime"))
    api(project(":kotlin-jvm-blocking-bridge-compiler"))

    // compileOnly("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}")
    compileOnly(fileTree("run/idea-sandbox/plugins/Kotlin/lib").filter {
        !it.name.contains("stdlib") && !it.name.contains("coroutines")
    })
}

group = "net.mamoe"
version = Versions.idePlugin

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set(Versions.intellij)
    downloadSources.set(true)
    updateSinceUntilBuild.set(false)

    sandboxDir.set(projectDir.resolve("run/idea-sandbox").absolutePath)

    plugins.set(
        listOf(
//            "org.jetbrains.kotlin:211-1.5.30-M1-release-141-IJ7442.40@eap",
            "java",
            "org.jetbrains.kotlin"
        )
    )
}

tasks.getByName("publishPlugin", org.jetbrains.intellij.tasks.PublishPluginTask::class) {
    val pluginKey = project.findProperty("jetbrains.hub.key")?.toString()
    if (pluginKey != null) {
        logger.info("Found jetbrains.hub.key")
        token.set(pluginKey)
    } else {
        logger.info("jetbrains.hub.key not found")
    }
}

tasks.withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask> {
    sinceBuild.set("213.0")
    untilBuild.set("221.9999")
    changeNotes.set(
        """
        See <a href="">Release notes</a>
    """.trimIndent()
    )
}

tasks.withType(KotlinJvmCompile::class) {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
}

//val theProject = project

//tasks.getByName("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
//    archiveClassifier.set("")
//    this.dependencyFilter.exclude {
//        it.name.contains("intellij", ignoreCase = true) || it.name.contains("idea", ignoreCase = true)
//    }
//    exclude {
//        // exclude ComponentRegistrar which is for CLI compiler.
//        it.name == "org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar" && !it.path.contains(theProject.path)
//    }
//}

//tasks.buildPlugin.get().dependsOn(tasks.shadowJar.get())
