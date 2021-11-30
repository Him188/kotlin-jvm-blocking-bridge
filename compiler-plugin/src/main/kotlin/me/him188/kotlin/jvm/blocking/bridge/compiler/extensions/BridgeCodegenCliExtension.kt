package me.him188.kotlin.jvm.blocking.bridge.compiler.extensions

import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.jvm.BridgeCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension

data class BridgeConfigurationImpl(
    override val unitCoercion: me.him188.kotlin.jvm.blocking.bridge.compiler.UnitCoercion,
    override val enableForModule: Boolean,
) : IBridgeConfiguration

interface IBridgeConfiguration {
    val unitCoercion: me.him188.kotlin.jvm.blocking.bridge.compiler.UnitCoercion
    val enableForModule: Boolean

    companion object {
        val Default = object : IBridgeConfiguration {
            override val unitCoercion: me.him188.kotlin.jvm.blocking.bridge.compiler.UnitCoercion get() = me.him188.kotlin.jvm.blocking.bridge.compiler.UnitCoercion.DEFAULT
            override val enableForModule: Boolean get() = false
        }
    }
}

/**
 * For JVM backend
 */
open class BridgeCodegenCliExtension(
    private val extension: IBridgeConfiguration,
) : ExpressionCodegenExtension {
    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        BridgeCodegen(codegen) { extension }.generate()
    }

    override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = true
}