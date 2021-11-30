package me.him188.kotlin.jvm.blocking.bridge.ide

import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.jvm.BridgeCodegen
import me.him188.kotlin.jvm.blocking.bridge.compiler.extensions.IBridgeConfiguration
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
        get() = false
}