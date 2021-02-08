package net.mamoe.kjbb.ide

import com.intellij.mock.MockProject
import net.mamoe.kjbb.compiler.JvmBlockingBridgeCompilerConfigurationKeys
import net.mamoe.kjbb.compiler.UnitCoercion
import net.mamoe.kjbb.compiler.backend.jvm.BridgeCodegen
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeDeclarationChecker
import net.mamoe.kjbb.compiler.extensions.JvmBlockingBridgeCodegenJvmExtension
import net.mamoe.kjbb.compiler.extensions.JvmBlockingBridgeIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

@Suppress("unused")
open class JvmBlockingBridgeIntelliJComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration,
    ) {
        // if (configuration[KEY_ENABLED] == false) {
        //     return
        // }

        val unitCoercion = configuration[JvmBlockingBridgeCompilerConfigurationKeys.UNIT_COERCION]
            ?.runCatching { UnitCoercion.valueOf(this) }?.getOrNull()
            ?: UnitCoercion.DEFAULT

        val enableForModule = configuration[JvmBlockingBridgeCompilerConfigurationKeys.ENABLE_FOR_MODULE]
            ?.toBooleanLenient()
            ?: false

        val ext = object : JvmBlockingBridgeCodegenJvmExtension(unitCoercion, enableForModule) {
            override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
                BridgeCodegen(codegen, CompilerContextIntelliJ, ext = this).generate()

            }
        }

        SyntheticResolveExtension.registerExtension(project, JvmBlockingBridgeResolveExtension())

        StorageComponentContainerContributor.registerExtension(project, object : StorageComponentContainerContributor {
            override fun registerModuleComponents(
                container: StorageComponentContainer,
                platform: TargetPlatform,
                moduleDescriptor: ModuleDescriptor,
            ) {
                container.useInstance(object : BlockingBridgeDeclarationChecker(moduleDescriptor.isIr(), ext) {
                    override fun isPluginEnabled(
                        descriptor: DeclarationDescriptor,
                    ): Boolean {
                        return descriptor.module.isBlockingBridgePluginEnabled()
                    }
                })
            }
        })
        CandidateInterceptor.registerExtension(project, JvmBlockingBridgeCallResolutionInterceptorExtension())
        IrGenerationExtension.registerExtension(project, JvmBlockingBridgeIrGenerationExtension(ext))
        ExpressionCodegenExtension.registerExtension(project, ext)
    }
}

