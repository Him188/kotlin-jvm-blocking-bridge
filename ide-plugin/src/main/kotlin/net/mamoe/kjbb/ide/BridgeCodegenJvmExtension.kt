package net.mamoe.kjbb.ide

import net.mamoe.kjbb.compiler.backend.jvm.BridgeCodegen
import net.mamoe.kjbb.compiler.extensions.IBridgeConfiguration
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi

/**
 * For JVM backend
 */
open class BridgeCodegenIdeExtension : ExpressionCodegenExtension {
    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        BridgeCodegen(codegen) { it.findPsi()?.bridgeConfiguration ?: IBridgeConfiguration.Default }.generate()
    }

    override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = true
}