package net.mamoe.kjbb

import net.mamoe.kjbb.ir.JvmBlockingBridgeIrGenerationExtension
import net.mamoe.kjbb.jvm.JvmBlockingBridgeCodegenJvmExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

@Suppress("unused")
class JvmBlockingBridgeComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        IrGenerationExtension.registerExtension(project, JvmBlockingBridgeIrGenerationExtension())
        ExpressionCodegenExtension.registerExtension(project, JvmBlockingBridgeCodegenJvmExtension())
    }
}


