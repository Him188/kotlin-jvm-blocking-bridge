package net.mamoe.kjbb.ide

import com.intellij.util.castSafelyTo
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.idea.facet.KotlinFacet

fun ModuleDescriptor.isIr(): Boolean {
    val compilerArguments = kotlinFacetSettings()?.compilerArguments ?: return false
    compilerArguments.pluginOptions
    return compilerArguments.castSafelyTo<K2JVMCompilerArguments>()?.useIR ?: return false
}


fun ModuleDescriptor.kotlinFacetSettings(): KotlinFacetSettings? {
    val module =
        getCapability(ModuleInfo.Capability)?.unwrapModuleSourceInfo()?.module ?: return null
    val facet = KotlinFacet.get(module) ?: return null
    return facet.configuration.settings
}


fun ModuleDescriptor.isBlockingBridgePluginEnabled(): Boolean {
    // /.m2/repository/net/mamoe/kotlin-jvm-blocking-bridge-compiler-embeddable/1.4.0/kotlin-jvm-blocking-bridge-compiler-embeddable-1.4.0.jar
    val pluginJpsJarName = "kotlin-jvm-blocking-bridge-compiler"
    val module =
        getCapability(ModuleInfo.Capability)?.unwrapModuleSourceInfo()?.module
            ?: return false
    val facet = KotlinFacet.get(module) ?: return false
    val pluginClasspath =
        facet.configuration.settings.compilerArguments?.pluginClasspaths ?: return false

    if (pluginClasspath.none { path -> path.contains(pluginJpsJarName) }) return false
    return true
}