package net.mamoe.kjbb

import net.mamoe.kjbb.compiler.extensions.JvmBlockingBridgeCommandLineProcessor.Companion.PLUGIN_ID
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

internal const val JBB_VERSION = "0.6.0"

open class BlockingBridgePluginExtension {
    var enabled: Boolean = true
}

internal fun BlockingBridgePluginExtension.toSubpluginOptionList(): List<SubpluginOption> {
    return listOf(
        SubpluginOption("enabled", enabled.toString())
    )
}

/*
class JvmBlockingBridgeGradleSubPlugin : KotlinGradleSubplugin<KotlinCompile> {
    override fun apply(
        project: Project,
        kotlinCompile: KotlinCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> {
        log("JvmBlockingBridgeGradleSubPlugin installed.")
        return listOf()
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = pluginArtifact

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean {
        return run {
            project.plugins.hasPlugin(JvmBlockingBridgeGradlePlugin::class.java)
                    && task is KotlinJvmCompile
        }.also { log("Application to ${task.name}: $it") }
    }

}
open class JvmBlockingBridgeGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        println("applied")
        target.dependencies.add("implementation", "net.mamoe:kotlin-jvm-blocking-bridge:$JBB_VERSION")
        //target.afterEvaluate { p ->
        //    p.dependencies.add("kotlinCompilerClasspath", "net.mamoe:kotlin-jvm-blocking-bridge-compiler:$JBB_VERSION")
        //}
    }
}
*/

private val pluginArtifact = SubpluginArtifact(
    groupId = PLUGIN_ID.substringBeforeLast('.'),
    artifactId = PLUGIN_ID.substringAfterLast('.'),
    version = JBB_VERSION
)  // .also { log("Adding: " + it.groupId + ":${it.artifactId}:${it.version}") }


open class JvmBlockingBridgeGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        // log("JvmBlockingBridgeGradlePlugin installed.")

        target.dependencies.add("implementation", "net.mamoe:kotlin-jvm-blocking-bridge:$JBB_VERSION")
        target.repositories.maven {
            it.setUrl("https://dl.bintray.com/mamoe/kotlin-jvm-blocking-bridge")
        }

        //target.afterEvaluate { p ->
        //    p.dependencies.add("kotlinCompilerClasspath", "net.mamoe:kotlin-jvm-blocking-bridge-compiler:$JBB_VERSION")
        //}

        target.extensions.create("blockingBridge", BlockingBridgePluginExtension::class.java)
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        return project.provider {
            mutableListOf<SubpluginOption>()
            //project.extensions.findByType(BlockingBridgePluginExtension::class.java)?.toSubpluginOptionList() ?: emptyList()
        }
    }

    override fun getPluginArtifact(): SubpluginArtifact = pluginArtifact

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {

        return kotlinCompilation.target.project.plugins.hasPlugin(JvmBlockingBridgeGradlePlugin::class.java)
                && when (kotlinCompilation.platformType) {
            KotlinPlatformType.jvm,
            KotlinPlatformType.androidJvm -> true
            else -> false
        }//.also { log("Application to ${kotlinCompilation.name}: $it") }
    }
}

private fun log(msg: String) = println("***JvmBlockingBridge: $msg")