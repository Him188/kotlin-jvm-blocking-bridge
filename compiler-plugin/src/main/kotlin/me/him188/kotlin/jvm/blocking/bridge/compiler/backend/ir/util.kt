@file:JvmName("JvmBlockingBridgeUtils")
@file:Suppress("unused") // for public API

package me.him188.kotlin.jvm.blocking.bridge.compiler.backend.ir

import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.jvm.GeneratedBlockingBridgeStubForResolution
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

/**
 * For annotation class
 */
fun IrClass.isJvmBlockingBridge(): Boolean =
    symbol.owner.fqNameWhenAvailable?.asString() == RuntimeIntrinsics.JvmBlockingBridgeFqName.asString()

/**
 * Filter by annotation `@JvmBlockingBridge`
 */
fun FunctionDescriptor.isJvmBlockingBridge(): Boolean = annotations.hasAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName)

/**
 * Filter by annotation `@JvmBlockingBridge`
 */
fun IrFunction.isJvmBlockingBridge(): Boolean = annotations.hasAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName)

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrFunction.isGeneratedBlockingBridgeStub(): Boolean =
    this.descriptor.getUserData(GeneratedBlockingBridgeStubForResolution) == true

fun IrSimpleFunction.isUserDeclaredFunction(): Boolean {
    return originalFunction.psiElement != null
}

fun IrSimpleFunction.findOverriddenDescriptorsHierarchically(filter: (IrSimpleFunction) -> Boolean): IrSimpleFunction? {
    for (override in this.allOverridden(false)) {
        if (filter(override)) {
            return override
        }
        val find = override.findOverriddenDescriptorsHierarchically(filter)
        if (find != null) return find
    }
    return null
}

internal fun IrAnnotationContainer.jvmBlockingBridgeAnnotation(): IrConstructorCall? =
    annotations.findAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName)

fun IrFunction.jvmBlockingBridgeAnnotationOnContainingClass(): IrConstructorCall? {
    val containingClass = parent

    if (containingClass is IrAnnotationContainer) {
        val annotation = containingClass.annotations.findAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName)
        if (annotation != null) return annotation
    }

    if (containingClass is IrClass) {
        val file = containingClass.parents.firstIsInstanceOrNull<IrFile>()
        val annotation = file?.annotations?.findAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName)
        if (annotation != null) return annotation
    }

    return null
}

internal val IrFunction.isFinal get() = this is IrSimpleFunction && this.modality == Modality.FINAL
internal val IrFunction.isOpen get() = this is IrSimpleFunction && this.modality == Modality.OPEN
internal val IrFunction.isAbstract get() = this is IrSimpleFunction && this.modality == Modality.ABSTRACT
