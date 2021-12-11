package me.him188.kotlin.jvm.blocking.bridge

import me.him188.kotlin.jvm.blocking.bridge.compiler.JvmBlockingBridgeCompilerConfigurationKeys.ENABLE_FOR_MODULE
import me.him188.kotlin.jvm.blocking.bridge.compiler.JvmBlockingBridgeCompilerConfigurationKeys.UNIT_COERCION
import me.him188.kotlin.jvm.blocking.bridge.compiler.extensions.BridgeCommandLineProcessor
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*


internal fun BlockingBridgePluginExtension.toSubpluginOptionList(): List<SubpluginOption> {
    return listOf(
        SubpluginOption(UNIT_COERCION.toString(), unitCoercion.name),
        SubpluginOption(ENABLE_FOR_MODULE.toString(), enableForModule.toString()),
    )
}

/**
 * Would download from maven central
 */
private val pluginArtifact = SubpluginArtifact(
    groupId = "me.him188",
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
                            implementation("me.him188:kotlin-jvm-blocking-bridge-runtime:$KJBB_VERSION")
                        }
                    }
                }
                if (applicableTargets.isNotEmpty()) {
                    target.repositories.mavenCentral()
                }

            }, onFailure = {
                if (kotlin.runCatching { target.extensions.getByType(KotlinJvmProjectExtension::class.java) }.isSuccess) {
                    // when JVM
                    target.dependencies.add(
                        "implementation",
                        "me.him188:kotlin-jvm-blocking-bridge-runtime:$KJBB_VERSION"
                    )
                    target.repositories.mavenCentral()
                } // else: neither JVM nor MPP. Don't apply
            })

        target.extensions.create("blockingBridge", BlockingBridgePluginExtension::class.java)
    }

    override fun getCompilerPluginId(): String = BridgeCommandLineProcessor.COMPILER_PLUGIN_ID

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