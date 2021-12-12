@file:Suppress("UNUSED_VARIABLE")

import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    id("me.him188.maven-central-publish")
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

    if (ideaActive) {
        when {
            Os.isFamily(Os.FAMILY_MAC) -> if (Os.isArch("aarch64")) macosArm64("native") else macosX64("native")
            Os.isFamily(Os.FAMILY_WINDOWS) -> mingwX64("native")
            else -> linuxX64("native")
        }
    } else {
        // 1.6.0
        val nativeTargets = arrayOf(
            "androidNativeArm32, androidNativeArm64, androidNativeX86, androidNativeX64",
            "iosArm32, iosArm64, iosX64, iosSimulatorArm64",
            "watchosArm32, watchosArm64, watchosX86, watchosX64, watchosSimulatorArm64",
            "tvosArm64, tvosX64, tvosSimulatorArm64",
            "macosX64, macosArm64",
            "linuxArm64, linuxArm32Hfp, linuxMips32, linuxMipsel32, linuxX64",
            "mingwX64, mingwX86",
            "wasm32"
        ).flatMap { it.split(", ") }
        presets.filter { it.name in nativeTargets }
            .forEach { preset ->
                val target = targetFromPreset(preset, preset.name)
                nativeMainSets.add(target.compilations[KotlinCompilation.MAIN_COMPILATION_NAME].kotlinSourceSets.first())
                nativeTestSets.add(target.compilations[KotlinCompilation.TEST_COMPILATION_NAME].kotlinSourceSets.first())
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
    this.workingDir = rootProject.buildDir.resolve("temp/pub/").apply { mkdirs() }
    useCentralS01()
    singleDevGithubProject("Him188", "kotlin-jvm-blocking-bridge")
    licenseApacheV2()

    publishPlatformArtifactsInRootModule = "jvm"
}