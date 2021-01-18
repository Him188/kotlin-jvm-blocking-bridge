package net.mamoe.kjbb.compiler.extensions

import com.google.auto.service.AutoService
import com.intellij.mock.MockProject
import net.mamoe.kjbb.compiler.JvmBlockingBridgeCompilerConfigurationKeys
import net.mamoe.kjbb.compiler.UnitCoercion
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeDeclarationChecker
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

@AutoService(ComponentRegistrar::class)
@Suppress("unused")
open class JvmBlockingBridgeComponentRegistrar @JvmOverloads constructor(
    private val overrideConfigurations: CompilerConfiguration? = null,
) : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration,
    ) {
        // if (configuratio[KEY_ENABLED] == false) {
        //     return
        // }

        //SyntheticResolveExtension.registerExtension(project, JvmBlockingBridgeResolveExtension())

        val actualConfiguration = overrideConfigurations ?: configuration

        println("actualConfiguration.toString(): $actualConfiguration")

        val unitCoercion = actualConfiguration[JvmBlockingBridgeCompilerConfigurationKeys.UNIT_COERCION]
            ?.runCatching { UnitCoercion.valueOf(this) }?.getOrNull()
            ?: UnitCoercion.DEFAULT

        StorageComponentContainerContributor.registerExtension(project, object : StorageComponentContainerContributor {
            override fun registerModuleComponents(
                container: StorageComponentContainer,
                platform: TargetPlatform,
                moduleDescriptor: ModuleDescriptor,
            ) {
                container.useInstance(BlockingBridgeDeclarationChecker(actualConfiguration[JVMConfigurationKeys.IR, false]))
            }
        })
        IrGenerationExtension.registerExtension(project, JvmBlockingBridgeIrGenerationExtension())
        ExpressionCodegenExtension.registerExtension(project, JvmBlockingBridgeCodegenJvmExtension(unitCoercion))
    }
}
