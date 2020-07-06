package net.mamoe.kjbb

import com.google.auto.service.AutoService
import net.mamoe.kjbb.ir.JvmBlockingBridgeIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

@Suppress("unused")
@AutoService(ComponentRegistrar::class)
class JvmBlockingBridgeComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) = IrGenerationExtension.registerExtension(
        project,
        JvmBlockingBridgeIrGenerationExtension()
    )
}


