package net.mamoe.kjbb.compiler.backend

import net.mamoe.kjbb.compiler.backend.ir.JvmBlockingBridgeIrGenerationExtension
import net.mamoe.kjbb.compiler.backend.jvm.JvmBlockingBridgeCodegenJvmExtension
import net.mamoe.kjbb.compiler.resolve.JvmBlockingBridgeResolveExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

@Suppress("unused")
open class JvmBlockingBridgeComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        if (configuration[KEY_ENABLED] == false) {
            return
        }

        SyntheticResolveExtension.registerExtension(project, JvmBlockingBridgeResolveExtension())

        IrGenerationExtension.registerExtension(project, JvmBlockingBridgeIrGenerationExtension())
        ExpressionCodegenExtension.registerExtension(project, JvmBlockingBridgeCodegenJvmExtension())
    }
}


