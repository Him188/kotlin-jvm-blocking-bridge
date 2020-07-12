package net.mamoe.kjbb

import io.github.classgraph.ClassGraph
import net.mamoe.kjbb.JvmBlockingBridgeCommandLineProcessor.Companion.PLUGIN_ID
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.File

internal const val JBB_VERSION = "0.1.12"

open class BlockingBridgePluginExtension {
    var enabled: Boolean = true
}

internal fun BlockingBridgePluginExtension.toSubpluginOptionList(): List<SubpluginOption> {
    return listOf(
        SubpluginOption("enabled", enabled.toString())
    )
}

open class JvmBlockingBridgeGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        log("JvmBlockingBridgeGradlePlugin installed.")

        target.dependencies.add("implementation", "net.mamoe:kotlin-jvm-blocking-bridge:$JBB_VERSION")

        //target.afterEvaluate { p ->
        //    p.dependencies.add("kotlinCompilerClasspath", "net.mamoe:kotlin-jvm-blocking-bridge-compiler:$JBB_VERSION")
        //}

        target.extensions.create("blockingBridge", BlockingBridgePluginExtension::class.java)
    }

    @Suppress("SameParameterValue")
    private fun classpathOf(dependency: String): File {
        val regex = Regex(".*${dependency.replace(':', '-')}.*")
        return ClassGraph().classpathFiles.first { classpath -> classpath.name.matches(regex) }
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        return project.provider {
            mutableListOf<SubpluginOption>()
            //project.extensions.findByType(BlockingBridgePluginExtension::class.java)?.toSubpluginOptionList() ?: emptyList()
        }
    }

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = PLUGIN_ID.substringBeforeLast('.'),
        artifactId = PLUGIN_ID.substringAfterLast('.'),
        version = JBB_VERSION
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {

        return kotlinCompilation.target.project.plugins.hasPlugin(JvmBlockingBridgeGradlePlugin::class.java)
                && when (kotlinCompilation.platformType) {
            KotlinPlatformType.jvm,
            KotlinPlatformType.androidJvm -> true
            else -> false
        }.also { log("Application to ${kotlinCompilation.name}: $it") }
    }
}

private fun log(msg: String) = println("***JvmBlockingBridge: $msg")