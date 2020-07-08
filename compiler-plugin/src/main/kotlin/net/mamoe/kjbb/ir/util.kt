@file:JvmName("JvmBlockingBridgeUtils")

package net.mamoe.kjbb.ir

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isClass
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.name.FqName
import kotlin.contracts.contract


val JVM_BLOCKING_BRIDGE_FQ_NAME = FqName("net.mamoe.kjbb.JvmBlockingBridge")
val GENERATED_BLOCKING_BRIDGE_FQ_NAME = FqName("net.mamoe.kjbb.GeneratedBlockingBridge")

/**
 * For annotation class
 */
fun IrClass.isJvmBlockingBridge(): Boolean = symbol.owner.fqNameWhenAvailable == JVM_BLOCKING_BRIDGE_FQ_NAME

/**
 * Filter by annotation `@JvmBlockingBridge`
 */
fun IrFunction.isJvmBlockingBridge(): Boolean = annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)

/**
 * Check whether a function is allowed to generate bridges with.
 *
 * The functions must
 * - be `final` or `open`
 * - have parent [IrClass]
 */
fun IrFunction.canGenerateJvmBlockingBridge(): Boolean {
    contract {
        returns() implies (this@canGenerateJvmBlockingBridge is IrSimpleFunction)
    }
    val parent = this.parent
    return this is IrSimpleFunction
            && (!isAbstract)
            && parent is IrClass
            && (parent.isClass || parent.isObject)
}


internal val IrFunction.isFinal get() = this is IrSimpleFunction && this.modality == Modality.FINAL
internal val IrFunction.isOpen get() = this is IrSimpleFunction && this.modality == Modality.OPEN
internal val IrFunction.isAbstract get() = this is IrSimpleFunction && this.modality == Modality.ABSTRACT
