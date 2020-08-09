@file:Suppress("UnstableApiUsage", "LocalVariableName")

import kotlin.reflect.KProperty

buildscript {
    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:6.0.0")
    }
}

plugins {
    kotlin("jvm") version Versions.kotlin apply false
    kotlin("kapt") version Versions.kotlin apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("com.gradle.plugin-publish") version "0.12.0" apply false
    id("com.jfrog.bintray") version Versions.bintray apply false
    id("com.bmuschko.nexus") version "2.3.1" apply false
    id("io.codearte.nexus-staging") version "0.11.0" apply false
    id("java")
    //id("com.github.johnrengelman.shadow") version "6.0.0" apply false
}

allprojects {
    group = "net.mamoe"
    description =
        "Kotlin compiler plugin for generating blocking bridges for calling suspend functions from Java with minimal effort"
    version = Versions.project

    repositories {
        mavenLocal()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        maven(url = "https://dl.bintray.com/kotlin/kotlin-dev")
        jcenter()
        mavenCentral()
    }
}

subprojects {
    afterEvaluate {
        setupKotlinSourceSetsSettings()
        tasks.withType(JavaCompile::class.java) {
            options.encoding = "UTF8"
        }
    }
}

afterEvaluate {
    // gradlew
    //:kotlin-jvm-blocking-bridge:ensureBintrayAvailable
    //:kotlin-jvm-blocking-bridge:clean
    //:kotlin-jvm-blocking-bridge-compiler:clean
    //:kotlin-jvm-blocking-bridge-gradle:clean
    //build
    //:kotlin-jvm-blocking-bridge:bintrayUpload
    //:kotlin-jvm-blocking-bridge-compiler:bintrayUpload
    //:kotlin-jvm-blocking-bridge-gradle:bintrayUpload
    //:kotlin-jvm-blocking-bridge-gradle:publishPlugins

    val `kotlin-jvm-blocking-bridge` by subprojects
    val `kotlin-jvm-blocking-bridge-compiler` by subprojects
    val `kotlin-jvm-blocking-bridge-gradle` by subprojects
    val `kotlin-jvm-blocking-bridge-intellij` by subprojects

    tasks.register("publish") {
        group = "publishing"
        dependsOn(`kotlin-jvm-blocking-bridge`.tasks["ensureBintrayAvailable"])
        dependsOn(`kotlin-jvm-blocking-bridge`.tasks["clean"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler`.tasks["clean"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["clean"])
        // don't clear IDE plugin, or IntelliJ sandbox caches will be removed.
        dependsOn(tasks["build"])
        dependsOn(`kotlin-jvm-blocking-bridge`.tasks["bintrayUpload"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler`.tasks["bintrayUpload"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["bintrayUpload"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["publishPlugins"])
        dependsOn(`kotlin-jvm-blocking-bridge-intellij`.tasks["buildPlugin"])
        // dependsOn(`kotlin-jvm-blocking-bridge-intellij`.tasks["publishPlugin"])
        // TODO: 2020/7/27 IDE plugin publish
    }

    tasks.register("publishWithoutClean") {
        group = "publishing"
        dependsOn(`kotlin-jvm-blocking-bridge`.tasks["ensureBintrayAvailable"])
        // don't clear IDE plugin, or IntelliJ sandbox caches will be removed.
        dependsOn(tasks["build"])
        dependsOn(`kotlin-jvm-blocking-bridge`.tasks["bintrayUpload"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler`.tasks["bintrayUpload"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["bintrayUpload"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["publishPlugins"])
        dependsOn(`kotlin-jvm-blocking-bridge-intellij`.tasks["buildPlugin"])
        // dependsOn(`kotlin-jvm-blocking-bridge-intellij`.tasks["publishPlugin"])
        // TODO: 2020/7/27 IDE plugin publish
    }
}

operator fun <E : Project> MutableSet<E>.getValue(e: E?, property: KProperty<*>): E {
    return this.firstOrNull { it.name == property.name }
        ?: error("Cannot find ${property.name} project in list ${this.joinToString()}")
}
