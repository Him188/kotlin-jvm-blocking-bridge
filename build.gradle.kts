@file:Suppress("UnstableApiUsage", "LocalVariableName")

import kotlin.reflect.KProperty

buildscript {
    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:6.0.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    }
}

plugins {
    //kotlin("jvm") version Versions.kotlin apply false
    id("io.github.karlatemp.publication-sign") version "1.0.0"
    kotlin("kapt") version Versions.kotlin apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("com.gradle.plugin-publish") version "0.12.0" apply false
    id("io.codearte.nexus-staging") version "0.22.0"
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
        mavenCentral()
    }
}

nexusStaging {
    packageGroup = rootProject.group.toString()
    username = System.getProperty("sonatype_key")
    password = System.getProperty("sonatype_password")
}

configure<io.github.karlatemp.publicationsign.PublicationSignExtension> {
    setupWorkflow {
        fastSetup("keys/key.pub", "keys/key.pri")
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

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
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
    val `kotlin-jvm-blocking-bridge-compiler-embeddable` by subprojects
    val `kotlin-jvm-blocking-bridge-gradle` by subprojects
    val `kotlin-jvm-blocking-bridge-intellij` by subprojects

    tasks.register("publishAll") {
        group = "publishing0"
        dependsOn(`kotlin-jvm-blocking-bridge`.tasks["clean"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler`.tasks["clean"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler-embeddable`.tasks["clean"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["clean"])
        // don't clear IDE plugin, or IntelliJ sandbox caches will be removed.
        dependsOn(tasks["build"])
        dependsOn(`kotlin-jvm-blocking-bridge`.tasks["publish"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler`.tasks["publish"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler-embeddable`.tasks["publish"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["publish"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["publishPlugins"])
    }

    tasks.register("publishAllToMavenLocal") {
        group = "publishing0"
        dependsOn(`kotlin-jvm-blocking-bridge`.tasks["clean"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler`.tasks["clean"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler-embeddable`.tasks["clean"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["clean"])
        // don't clear IDE plugin, or IntelliJ sandbox caches will be removed.
        dependsOn(tasks["build"])
        dependsOn(`kotlin-jvm-blocking-bridge`.tasks["publishToMavenLocal"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler`.tasks["publishToMavenLocal"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler-embeddable`.tasks["publishToMavenLocal"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["publishToMavenLocal"])
    }

    tasks.register("publishAllWithoutClean") {
        group = "publishing0"
        // don't clear IDE plugin, or IntelliJ sandbox caches will be removed.
        dependsOn(tasks["build"])
        dependsOn(`kotlin-jvm-blocking-bridge`.tasks["publish"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler`.tasks["publish"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler-embeddable`.tasks["publish"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["publish"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["publishPlugins"])
    }

    tasks.register("publishAllToMavenLocalWithoutClean") {
        group = "publishing0"
        // don't clear IDE plugin, or IntelliJ sandbox caches will be removed.
        dependsOn(tasks["build"])
        dependsOn(`kotlin-jvm-blocking-bridge`.tasks["publishToMavenLocal"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler`.tasks["publishToMavenLocal"])
        dependsOn(`kotlin-jvm-blocking-bridge-compiler-embeddable`.tasks["publishToMavenLocal"])
        dependsOn(`kotlin-jvm-blocking-bridge-gradle`.tasks["publishToMavenLocal"])
    }
}

operator fun <E : Project> MutableSet<E>.getValue(e: E?, property: KProperty<*>): E {
    return this.firstOrNull { it.name == property.name }
        ?: error("Cannot find ${property.name} project in list ${this.joinToString()}")
}

