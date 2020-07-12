package net.mamoe.kjbb.jvm

import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension

/**
 * For JVM backend
 */
open class JvmBlockingBridgeCodegenJvmExtension : ExpressionCodegenExtension {
    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        BridgeCodegen(codegen).generate()
    }

    override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = false
}