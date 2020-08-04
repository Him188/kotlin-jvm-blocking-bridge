package net.mamoe.kjbb.ide

import com.google.auto.service.AutoService
import com.intellij.mock.MockProject
import net.mamoe.kjbb.compiler.backend.jvm.BridgeCodegen
import net.mamoe.kjbb.compiler.extensions.JvmBlockingBridgeIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

@AutoService(ComponentRegistrar::class)
@Suppress("unused")
open class JvmBlockingBridgeIntelliJComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        println("registerProjectComponents")
        // if (configuration[KEY_ENABLED] == false) {
        //     return
        // }

        SyntheticResolveExtension.registerExtension(project, JvmBlockingBridgeResolveExtension())

        CandidateInterceptor.registerExtension(project, JvmBlockingBridgeCallResolutionInterceptorExtension())
        IrGenerationExtension.registerExtension(project, JvmBlockingBridgeIrGenerationExtension())
        ExpressionCodegenExtension.registerExtension(project, object : ExpressionCodegenExtension {
            override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
                BridgeCodegen(codegen, CompilerContextIntelliJ).generate()
            }

            override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean get() = true
        })
    }
}

