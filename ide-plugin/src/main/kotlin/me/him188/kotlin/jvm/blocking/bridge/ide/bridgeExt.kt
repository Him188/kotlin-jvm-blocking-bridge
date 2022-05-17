package me.him188.kotlin.jvm.blocking.bridge.ide

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.intellij.util.castSafelyTo
import me.him188.kotlin.jvm.blocking.bridge.compiler.extensions.BridgeCommandLineProcessor
import me.him188.kotlin.jvm.blocking.bridge.compiler.extensions.IBridgeConfiguration
import me.him188.kotlin.jvm.blocking.bridge.compiler.extensions.createBridgeConfig
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.toDescriptor
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.util.module

val Module.bridgeConfiguration
    get() = useBridgeCacheOrInit { it.config } ?: IBridgeConfiguration.Default

val PsiElement.bridgeConfiguration
    get() = module?.bridgeConfiguration ?: IBridgeConfiguration.Default

val Module.isIr
    get() = useBridgeCacheOrInit { it.isIr } ?: false

val PsiElement.isIr
    get() = module?.isIr ?: false

val Module.isBridgeCompilerEnabled
    get() = useBridgeCacheOrInit { it.compilerEnabled } ?: false

val PsiElement.isBridgeCompilerEnabled
    get() = module?.isBridgeCompilerEnabled ?: false


inline fun <R> Module.useBridgeCacheOrInit(
    useCache: (cache: BridgeModuleCacheService) -> R,
): R? {
    val module = this
    val moduleDescriptor = module.toDescriptor() ?: return null
    val cache = module.getService(BridgeModuleCacheService::class.java)
    if (cache.initialized) {
        return useCache(cache)
    }

    cache.isIr = moduleDescriptor.isIr()
    cache.config = moduleDescriptor.createBridgeConfig() ?: IBridgeConfiguration.Default
    cache.compilerEnabled = moduleDescriptor.isBlockingBridgePluginEnabled()
    cache.initialized = true

    return useCache(cache)
}


fun ModuleDescriptor.isIr(): Boolean {
    val compilerArguments = kotlinFacetSettings()?.compilerArguments ?: return true // true by default
    if (compilerArguments.castSafelyTo<K2JVMCompilerArguments>()?.useOldBackend == true) {
        return false
    }
    return true
}

//
fun ModuleDescriptor.createBridgeConfig(): IBridgeConfiguration? {
    return kotlinFacetSettings()?.compilerArguments?.kjbbCompilerConfiguration()?.createBridgeConfig()
}

private fun CommonCompilerArguments.kjbbCompilerConfiguration(): CompilerConfiguration? {
    val pluginOptions = pluginOptions ?: return null

    fun findOption(option: CliOption): String? {
        return pluginOptions.find { it.startsWith("plugin:${BridgeCommandLineProcessor.COMPILER_PLUGIN_ID}:${option.optionName}=") }
            ?.substringAfter('=', "")
    }

    val processor = BridgeCommandLineProcessor()
    val configuration = CompilerConfiguration()

    for (pluginOption in processor.pluginOptions) {
        val find = findOption(pluginOption)
        if (find != null) {
            processor.processOption(pluginOption as AbstractCliOption, find, configuration)
        }
    }
    return configuration
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