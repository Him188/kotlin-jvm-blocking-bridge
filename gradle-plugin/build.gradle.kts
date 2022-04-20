import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.targets

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("java-gradle-plugin")
    `maven-publish`
    id("com.gradle.plugin-publish")
}

kotlin.targets.asSequence()
    .flatMap { it.compilations }
    .filter { it.platformType == org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm }
    .map { it.kotlinOptions }
    .filterIsInstance<org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions>()
    .forEach { it.jvmTarget = "1.8" }

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(gradleApi())
    compileOnly(kotlin("gradle-plugin-api"))
    compileOnly(kotlin("gradle-plugin"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${Versions.kotlin}")

    api(project(":kotlin-jvm-blocking-bridge-compiler"))
}

pluginBundle {
    website = "https://github.com/him188/kotlin-jvm-blocking-bridge"
    vcsUrl = "https://github.com/him188/kotlin-jvm-blocking-bridge.git"
    tags = listOf("kotlin", "jvm-blocking-bridge")
}

gradlePlugin {
    plugins {
        create("kotlinJvmBlockingBridge") {
            id = "me.him188.kotlin-jvm-blocking-bridge"
            displayName = "Kotlin JVM Blocking Bridge"
            description = project.description
            implementationClass = "me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridgeGradlePlugin"
        }
    }
}

tasks.register("updateKJBBVersion") {
    doLast {
        project.projectDir.resolve("src/main/kotlin/me/him188/kotlin/jvm/blocking/bridge")
            .resolve("VersionGenerated.kt")
            .apply { createNewFile() }
            .writeText(
                """
                package me.him188.kotlin.jvm.blocking.bridge

                internal const val KJBB_VERSION = "${Versions.project}"
            """.trimIndent()
            )
    }
}

tasks.getByName("compileKotlin").dependsOn("updateKJBBVersion")

/*
tasks.getByName("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveClassifier.set("")
}

tasks.publishPlugins.get().dependsOn(tasks.shadowJar.get())*/

