@file:JvmName("JvmBlockingBridgeUtils")
@file:Suppress("unused") // for public API

package net.mamoe.kjbb.compiler.backend.ir

import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.kjbb.compiler.backend.jvm.BlockingBridgeTestResult
import net.mamoe.kjbb.compiler.backend.jvm.canGenerateJvmBlockingBridge
import org.jetbrains.kotlin.codegen.topLevelClassAsmType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName


val JVM_BLOCKING_BRIDGE_FQ_NAME = FqName(JvmBlockingBridge::class.qualifiedName!!)

@Suppress(
    "INVISIBLE_REFERENCE",
    "EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL",
    "DEPRECATION_ERROR"
)
val GENERATED_BLOCKING_BRIDGE_FQ_NAME = FqName(net.mamoe.kjbb.GeneratedBlockingBridge::class.qualifiedName!!)

val JVM_BLOCKING_BRIDGE_ASM_TYPE = JVM_BLOCKING_BRIDGE_FQ_NAME.topLevelClassAsmType()
val GENERATED_BLOCKING_BRIDGE_ASM_TYPE = GENERATED_BLOCKING_BRIDGE_FQ_NAME.topLevelClassAsmType()

/**
 * For annotation class
 */
fun IrClass.isJvmBlockingBridge(): Boolean =
    symbol.owner.fqNameWhenAvailable?.asString() == JVM_BLOCKING_BRIDGE_FQ_NAME.asString()

/**
 * Filter by annotation `@JvmBlockingBridge`
 */
fun FunctionDescriptor.isJvmBlockingBridge(): Boolean = annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)

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
fun IrFunction.canGenerateJvmBlockingBridge(): BlockingBridgeTestResult {
    return descriptor.canGenerateJvmBlockingBridge()
    /*
    contract {
        returns() implies (this@canGenerateJvmBlockingBridge is IrSimpleFunction)
    }
    val parent = this.parent
    return this is IrSimpleFunction
            && (!isAbstract)
            && parent is IrClass
            && (parent.isClass || parent.isObject)*/
}


internal val IrFunction.isFinal get() = this is IrSimpleFunction && this.modality == Modality.FINAL
internal val IrFunction.isOpen get() = this is IrSimpleFunction && this.modality == Modality.OPEN
internal val IrFunction.isAbstract get() = this is IrSimpleFunction && this.modality == Modality.ABSTRACT
