@file:Suppress("UnstableApiUsage", "LocalVariableName")

import me.him188.maven.central.publish.gradle.MavenCentralPublishExtension

buildscript {
    dependencies {
//        classpath("com.github.jengelman.gradle.plugins:shadow:7.0.0")
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    }
}

plugins {
    kotlin("multiplatform") apply false
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
    kotlin("kapt") apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("com.gradle.plugin-publish") version "0.12.0" apply false
    id("io.codearte.nexus-staging") version "0.22.0"
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.0" apply false
}

allprojects {
    group = Versions.publicationGroup
    description =
        "Kotlin compiler plugin for generating blocking bridges for calling suspend functions from Java with minimal effort"
    version = Versions.project

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

nexusStaging {
    packageGroup = rootProject.group.toString()
    username = System.getProperty("sonatype_key") ?: project.findProperty("sonatype.key")?.toString()
    password = System.getProperty("sonatype_password") ?: project.findProperty("sonatype.password")?.toString()
}

subprojects {
    afterEvaluate {
        setupKotlinSourceSetsSettings()
    }
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

allprojects {
    if (extensions.findByType(MavenCentralPublishExtension::class) != null) {
        mavenCentralPublish {
            useCentralS01()
        }
    }
}