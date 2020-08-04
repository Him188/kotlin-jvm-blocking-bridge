package net.mamoe.kjbb.compiler.extensions

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(ComponentRegistrar::class)
@Suppress("unused")
open class JvmBlockingBridgeComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        println("registerProjectComponents")
        // if (configuration[KEY_ENABLED] == false) {
        //     return
        // }

        //SyntheticResolveExtension.registerExtension(project, JvmBlockingBridgeResolveExtension())

        IrGenerationExtension.registerExtension(project, JvmBlockingBridgeIrGenerationExtension())
        ExpressionCodegenExtension.registerExtension(project, JvmBlockingBridgeCodegenJvmExtension())
    }
}
