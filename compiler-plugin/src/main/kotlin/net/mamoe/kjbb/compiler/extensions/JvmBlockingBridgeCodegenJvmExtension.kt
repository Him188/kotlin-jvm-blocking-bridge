package net.mamoe.kjbb.compiler.extensions

import com.google.auto.service.AutoService
import net.mamoe.kjbb.compiler.UnitCoercion
import net.mamoe.kjbb.compiler.backend.jvm.BridgeCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension

/**
 * For JVM backend
 */
@AutoService(ExpressionCodegenExtension::class)
open class JvmBlockingBridgeCodegenJvmExtension @JvmOverloads constructor(
    private val unitCoercion: UnitCoercion = UnitCoercion.DEFAULT,
) :
    ExpressionCodegenExtension {

    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        BridgeCodegen(codegen, unitCoercion = unitCoercion).generate()
    }

    override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = true
}