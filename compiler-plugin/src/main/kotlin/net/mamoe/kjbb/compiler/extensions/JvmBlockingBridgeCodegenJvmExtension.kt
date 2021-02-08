package net.mamoe.kjbb.compiler.extensions

import com.google.auto.service.AutoService
import net.mamoe.kjbb.compiler.UnitCoercion
import net.mamoe.kjbb.compiler.backend.jvm.BridgeCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension

interface IJvmBlockingBridgeCodegenJvmExtension {
    val unitCoercion: UnitCoercion
    val enableForModule: Boolean

    companion object {
        val Default = object : IJvmBlockingBridgeCodegenJvmExtension {
            override val unitCoercion: UnitCoercion get() = UnitCoercion.DEFAULT
            override val enableForModule: Boolean get() = false
        }
    }
}

/**
 * For JVM backend
 */
@AutoService(ExpressionCodegenExtension::class)
open class JvmBlockingBridgeCodegenJvmExtension @JvmOverloads constructor(
    override val unitCoercion: UnitCoercion = UnitCoercion.DEFAULT,
    override val enableForModule: Boolean = false,
) : ExpressionCodegenExtension, IJvmBlockingBridgeCodegenJvmExtension {

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        BridgeCodegen(codegen, ext = this).generate()
    }

    override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = true
}