package net.mamoe.kjbb.compiler.extensions

import net.mamoe.kjbb.compiler.UnitCoercion
import net.mamoe.kjbb.compiler.backend.jvm.BridgeCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension

data class BridgeConfigurationImpl(
    override val unitCoercion: UnitCoercion,
    override val enableForModule: Boolean,
) : IBridgeConfiguration

interface IBridgeConfiguration {
    val unitCoercion: UnitCoercion
    val enableForModule: Boolean

    companion object {
        val Default = object : IBridgeConfiguration {
            override val unitCoercion: UnitCoercion get() = UnitCoercion.DEFAULT
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