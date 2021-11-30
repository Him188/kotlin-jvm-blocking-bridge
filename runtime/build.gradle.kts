@file:Suppress("UNUSED_VARIABLE")

import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset

plugins {
    id("net.mamoe.maven-central-publish")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    explicitApi()


    jvm()
    js {
        useCommonJs()
    }


    val ideaActive = System.getProperty("idea.active") == "true"

    val nativeMainSets = mutableListOf<KotlinSourceSet>()
    val nativeTestSets = mutableListOf<KotlinSourceSet>()

    val addTarget = { preset: KotlinTargetPreset<*> ->
        val target = targetFromPreset(preset, preset.name)
        nativeMainSets.add(target.compilations["main"].kotlinSourceSets.first())
        nativeTestSets.add(target.compilations["test"].kotlinSourceSets.first())
    }

    if (ideaActive) {
        when {
            Os.isFamily(Os.FAMILY_MAC) -> if (Os.isArch("aarch64")) macosArm64("native") else macosX64("native")
            Os.isFamily(Os.FAMILY_WINDOWS) -> mingwX64("native")
            else -> linuxX64("native")
        }
    } else {
        presets.forEach { preset ->
            addTarget(preset)
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:5.2.0")
                implementation("org.junit.jupiter:junit-jupiter-engine:5.2.0")
            }
        }

        val jsMain by getting

        if (!ideaActive) {
            configure(nativeMainSets) {
                dependsOn(sourceSets.maybeCreate("nativeMain"))
            }

            configure(nativeTestSets) {
                dependsOn(sourceSets.maybeCreate("nativeTest"))
            }
        }
    }
}

mavenCentralPublish {
    artifactId = "kotlin-jvm-blocking-bridge-runtime"
    packageGroup = Versions.publicationGroup
    singleDevGithubProject("Him188", "kotlin-jvm-blocking-bridge")
    licenseApacheV2()

    publishPlatformArtifactsInRootModule = "jvm"
}