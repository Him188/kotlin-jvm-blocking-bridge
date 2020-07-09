@file:Suppress("UnstableApiUsage")

plugins {
    kotlin("jvm") version Versions.kotlin apply false
    kotlin("kapt") version Versions.kotlin apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("com.jfrog.bintray") version Versions.bintray apply false
    id("java")
}

allprojects {
    group = "net.mamoe"
    description =
        "Kotlin compiler plugin that can generate a blocking bridge for calling suspend functions from Java with minimal effort"
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