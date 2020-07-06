@file:Suppress("UnstableApiUsage")

plugins {
    kotlin("jvm") version Versions.kotlin apply false
    kotlin("kapt") version Versions.kotlin apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("java")
    `maven-publish`
    id("com.jfrog.bintray") version Versions.bintray apply false
}

tasks.withType(JavaCompile::class.java) {
    options.encoding = "UTF8"
}

allprojects {
    group = "net.mamoe"

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
    }
}