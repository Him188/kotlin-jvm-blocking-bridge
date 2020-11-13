package net.mamoe.kjbb.ide

import com.intellij.util.castSafelyTo
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeDeclarationChecker
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class JvmBlockingBridgeIDEContainerContributor : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor,
    ) {
        fun isIr(): Boolean {
            val module =
                moduleDescriptor.getCapability(ModuleInfo.Capability)?.unwrapModuleSourceInfo()?.module ?: return false
            val facet = KotlinFacet.get(module) ?: return false
            val compilerArguments = facet.configuration.settings.compilerArguments ?: return false
            return compilerArguments.castSafelyTo<K2JVMCompilerArguments>()?.useIR ?: return false
        }

        container.useInstance(object : BlockingBridgeDeclarationChecker(isIr()) {
            override fun checkIsPluginEnabled(
                descriptor: DeclarationDescriptor,
            ): Boolean {
                return descriptor.module.isBlockingBridgePluginEnabled()
            }
        })
    }
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