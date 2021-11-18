@file:Suppress("UnstableApiUsage", "LocalVariableName")

buildscript {
    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:6.0.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    }
}

plugins {
    //kotlin("jvm") version Versions.kotlin apply false
    id("io.github.karlatemp.publication-sign") version "1.1.0"
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
        mavenCentral()
        gradlePluginPortal()
    }
}

nexusStaging {
    packageGroup = rootProject.group.toString()
    username = System.getProperty("sonatype_key") ?: project.findProperty("sonatype.key")?.toString()
    password = System.getProperty("sonatype_password") ?: project.findProperty("sonatype.password")?.toString()
}

configure<io.github.karlatemp.publicationsign.PublicationSignExtension> {
    setupWorkflow {
        System.getProperty("signer.workdir")?.let { workingDir = File(it) }
        fastSetup("keys/keys.pub", "keys/keys.pri")
    }
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