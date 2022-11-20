package me.him188.kotlin.jvm.blocking.bridge.compiler.extensions

import com.google.auto.service.AutoService
import com.intellij.mock.MockProject
import me.him188.kotlin.jvm.blocking.bridge.compiler.diagnostic.BlockingBridgeDeclarationChecker
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
@Suppress("unused")
open class BridgeComponentRegistrar @JvmOverloads constructor(
    private val overrideConfigurations: CompilerConfiguration? = null,
) : CompilerPluginRegistrar() {
    override val supportsK2: Boolean get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // if (configuratio[KEY_ENABLED] == false) {
        //     return
        // }

        //SyntheticResolveExtension.registerExtension(project, JvmBlockingBridgeResolveExtension())

        val actualConfiguration = overrideConfigurations ?: configuration

        val ext = actualConfiguration.createBridgeConfig()

        // println("actualConfiguration.toString(): $actualConfiguration")

        StorageComponentContainerContributor.registerExtension(object : StorageComponentContainerContributor {
            override fun registerModuleComponents(
                container: StorageComponentContainer,
                platform: TargetPlatform,
                moduleDescriptor: ModuleDescriptor,
            ) {
                container.useInstance(
                    BlockingBridgeDeclarationChecker(actualConfiguration[JVMConfigurationKeys.IR, false]) { ext }
                )
            }
        })
        IrGenerationExtension.registerExtension(JvmBlockingBridgeIrGenerationExtension(ext))
        ExpressionCodegenExtension.registerExtension(BridgeCodegenCliExtension(ext))
    }
}

@OptIn(ExperimentalCompilerApi::class)
internal class BridgeComponentRegistrarForTest @JvmOverloads constructor(
    private val overrideConfigurations: CompilerConfiguration? = null,
) : ComponentRegistrar {
    override val supportsK2: Boolean get() = true

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        // if (configuratio[KEY_ENABLED] == false) {
        //     return
        // }

        //SyntheticResolveExtension.registerExtension(project, JvmBlockingBridgeResolveExtension())

        val actualConfiguration = overrideConfigurations ?: configuration

        val ext = actualConfiguration.createBridgeConfig()

        // println("actualConfiguration.toString(): $actualConfiguration")

        StorageComponentContainerContributor.registerExtension(project, object : StorageComponentContainerContributor {
            override fun registerModuleComponents(
                container: StorageComponentContainer,
                platform: TargetPlatform,
                moduleDescriptor: ModuleDescriptor,
            ) {
                container.useInstance(
                    BlockingBridgeDeclarationChecker(actualConfiguration[JVMConfigurationKeys.IR, false]) { ext }
                )
            }
        })
        IrGenerationExtension.registerExtension(project, JvmBlockingBridgeIrGenerationExtension(ext))
        ExpressionCodegenExtension.registerExtension(project, BridgeCodegenCliExtension(ext))
    }
}


fun CompilerConfiguration.createBridgeConfig(): BridgeConfigurationImpl {
    val actualConfiguration = this

    val unitCoercion =
        actualConfiguration[me.him188.kotlin.jvm.blocking.bridge.compiler.JvmBlockingBridgeCompilerConfigurationKeys.UNIT_COERCION]
            ?.runCatching { me.him188.kotlin.jvm.blocking.bridge.compiler.UnitCoercion.valueOf(this) }?.getOrNull()
            ?: me.him188.kotlin.jvm.blocking.bridge.compiler.UnitCoercion.DEFAULT

    val enableForModule =
        actualConfiguration[me.him188.kotlin.jvm.blocking.bridge.compiler.JvmBlockingBridgeCompilerConfigurationKeys.ENABLE_FOR_MODULE]
            ?.toBooleanLenient()
            ?: false

    return BridgeConfigurationImpl(unitCoercion, enableForModule)
}
