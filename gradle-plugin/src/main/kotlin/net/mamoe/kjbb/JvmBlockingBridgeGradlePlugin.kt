package net.mamoe.kjbb

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

internal const val JBB_VERSION = "0.1.3"

open class JvmBlockingBridgeGradlePlugin : KotlinCompilerPluginSupportPlugin {
    private lateinit var project: Project

    override fun apply(target: Project) {
        project = target
    }

    override fun getCompilerPluginId(): String = "net.mamoe.kotlin-jvm-blocking-bridge"

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        return project.provider { mutableListOf<SubpluginOption>() }
    }

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "net.mamoe",
        artifactId = "kotlin-jvm-blocking-bridge",
        version = JBB_VERSION
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return when (kotlinCompilation.platformType) {
            KotlinPlatformType.jvm,
            KotlinPlatformType.androidJvm -> true
            else -> false
        }
    }
}