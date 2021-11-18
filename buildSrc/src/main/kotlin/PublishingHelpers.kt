@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "RemoveRedundantBackticks")

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("NOTHING_TO_INLINE")
inline fun Project.setupKotlinSourceSetsSettings() {
    kotlin.runCatching {
        extensions.getByType(KotlinProjectExtension::class.java)
    }.getOrNull()?.run {
        sourceSets.all {

            languageSettings.apply {
                progressiveMode = true

                optIn("kotlin.Experimental")
                optIn("kotlin.RequiresOptIn")

                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlin.experimental.ExperimentalTypeInference")
                optIn("kotlin.contracts.ExperimentalContracts")
            }
        }
    }

    kotlin.runCatching {
        tasks.withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    kotlin.runCatching { tasks.getByName("test", Test::class) }.getOrNull()?.apply {
        useJUnitPlatform()
    }
}