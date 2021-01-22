package net.mamoe.kjbb

import net.mamoe.kjbb.compiler.JvmBlockingBridgeCompilerConfigurationKeys.UNIT_COERCION
import net.mamoe.kjbb.compiler.extensions.JvmBlockingBridgeCommandLineProcessor
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*

internal const val KJBB_VERSION = "1.7.4"

internal fun BlockingBridgePluginExtension.toSubpluginOptionList(): List<SubpluginOption> {
    return listOf(
        SubpluginOption(UNIT_COERCION.toString(), unitCoercion.name)
    )
}

/**
 * Would download from jcenter
 */
private val pluginArtifact = SubpluginArtifact(
    groupId = "net.mamoe",
    artifactId = "kotlin-jvm-blocking-bridge-compiler-embeddable",
    version = KJBB_VERSION
)  // .also { log("Adding: " + it.groupId + ":${it.artifactId}:${it.version}") }


open class JvmBlockingBridgeGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        // log("JvmBlockingBridgeGradlePlugin installed.")

        kotlin.runCatching { target.extensions.getByType(KotlinMultiplatformExtension::class.java) }
            .fold(onSuccess = { kotlin ->
                // when MPP

                val applicableTargets =
                    kotlin.targets.filter { it.platformType == KotlinPlatformType.common }

                for (applicableTarget in applicableTargets) {
                    applicableTarget.compilations.flatMap { it.allKotlinSourceSets }.forEach {
                        it.dependencies {
                            implementation("net.mamoe:kotlin-jvm-blocking-bridge:$KJBB_VERSION")
                        }
                    }
                }
                if (applicableTargets.isNotEmpty()) {
                    target.repositories.maven { it.setUrl("https://dl.bintray.com/mamoe/kotlin-jvm-blocking-bridge") }
                }

            }, onFailure = {
                if (kotlin.runCatching { target.extensions.getByType(KotlinJvmProjectExtension::class.java) }.isSuccess) {
                    // when JVM
                    target.dependencies.add("implementation", "net.mamoe:kotlin-jvm-blocking-bridge:$KJBB_VERSION")
                    target.repositories.maven { it.setUrl("https://dl.bintray.com/mamoe/kotlin-jvm-blocking-bridge") }
                } // else: neither JVM nor MPP. Don't apply
            })

        target.extensions.create("blockingBridge", BlockingBridgePluginExtension::class.java)
    }

    override fun getCompilerPluginId(): String = JvmBlockingBridgeCommandLineProcessor.COMPILER_PLUGIN_ID

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val ext: BlockingBridgePluginExtension? =
            project.extensions.findByType(BlockingBridgePluginExtension::class.java)
        return project.provider {
            mutableListOf<SubpluginOption>()
            ext?.toSubpluginOptionList() ?: emptyList()
        }
    }

    override fun getPluginArtifact(): SubpluginArtifact = pluginArtifact

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {

        return kotlinCompilation.target.project.plugins.hasPlugin(JvmBlockingBridgeGradlePlugin::class.java)
                && when (kotlinCompilation.platformType) {
            KotlinPlatformType.jvm,
            KotlinPlatformType.androidJvm,
            KotlinPlatformType.common,
            -> true
            else -> false
        }//.also { log("Application to ${kotlinCompilation.name} (${kotlinCompilation.platformType}): $it") }
    }
}

private fun log(msg: String) = println("***JvmBlockingBridge: $msg")