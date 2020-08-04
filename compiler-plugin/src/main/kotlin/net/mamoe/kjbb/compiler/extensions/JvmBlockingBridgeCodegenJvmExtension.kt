package net.mamoe.kjbb.compiler.extensions

import com.google.auto.service.AutoService
import net.mamoe.kjbb.compiler.backend.jvm.BridgeCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension

/**
 * For JVM backend
 */
@AutoService(ExpressionCodegenExtension::class)
open class JvmBlockingBridgeCodegenJvmExtension :
    ExpressionCodegenExtension {

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        if (kotlin.runCatching { Class.forName("org.jetbrains.kotlin.com.intellij.psi.PsiElement") }
                .isFailure) {
            // Running in IDE
            return
        }
        BridgeCodegen(codegen).generate()
    }

    override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = true
}