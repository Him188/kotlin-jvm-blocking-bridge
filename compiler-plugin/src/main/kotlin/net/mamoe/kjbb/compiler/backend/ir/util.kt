@file:JvmName("JvmBlockingBridgeUtils")
@file:Suppress("unused") // for public API

package net.mamoe.kjbb.compiler.backend.ir

import com.intellij.psi.PsiElement
import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.kjbb.compiler.backend.jvm.BlockingBridgeAnalyzeResult
import net.mamoe.kjbb.compiler.backend.jvm.GeneratedBlockingBridgeStubForResolution
import net.mamoe.kjbb.compiler.backend.jvm.isJvm8OrHigher
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.jvm.codegen.psiElement
import org.jetbrains.kotlin.codegen.topLevelClassAsmType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.effectiveVisibility
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
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

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrFunction.isGeneratedBlockingBridgeStub(): Boolean =
    this.descriptor.getUserData(GeneratedBlockingBridgeStubForResolution) == true

/**
 * Check whether a function is allowed to generate bridges with.
 *
 * The functions must
 * - be `final` or `open`
 * - have parent [IrClass]
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrFunction.analyzeCapabilityForGeneratingBridges(): BlockingBridgeAnalyzeResult {
    val jvmBlockingBridgeAnnotation = jvmBlockingBridgeAnnotation()
        ?: descriptor.findPsi() ?: return BlockingBridgeAnalyzeResult.MISSING_ANNOTATION_PSI

    if (isGeneratedBlockingBridgeStub()) return BlockingBridgeAnalyzeResult.FROM_STUB
    if (visibility.normalize().effectiveVisibility(descriptor, true).privateApi)
        return BlockingBridgeAnalyzeResult.RedundantForPrivateDeclarations(jvmBlockingBridgeAnnotation)
    val containingClass = parentClassOrNull
    if (containingClass?.isInline == true)
        return BlockingBridgeAnalyzeResult.InlineClassesNotSupported(jvmBlockingBridgeAnnotation,
            containingClass.descriptor)
    allParameters.firstOrNull { it.type.isInlined() }?.let { param ->
        return BlockingBridgeAnalyzeResult.InlineClassesNotSupported(
            param.psiElement ?: jvmBlockingBridgeAnnotation, param.descriptor)
    }

    if (containingClass?.isInterface == true) { // null means top-level, which is also accepted
        if (module.platform?.isJvm8OrHigher() != true)
            return BlockingBridgeAnalyzeResult.InterfaceNotSupported(jvmBlockingBridgeAnnotation)
    } else {
        if (!isSuspend || name.isSpecial || !annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)) {
            return BlockingBridgeAnalyzeResult.Inapplicable(jvmBlockingBridgeAnnotation)
        }

        val overridden = originalFunction.realOverrideTarget

        if (overridden === this || overridden.analyzeCapabilityForGeneratingBridges() != BlockingBridgeAnalyzeResult.ALLOWED)// overriding a super function
            return BlockingBridgeAnalyzeResult.OriginFunctionOverridesSuperMember(overridden.descriptor, isReal)
    }

    return BlockingBridgeAnalyzeResult.ALLOWED
}


internal fun IrAnnotationContainer.jvmBlockingBridgeAnnotation(): PsiElement? =
    annotations.findAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)?.psiElement

internal val IrFunction.isFinal get() = this is IrSimpleFunction && this.modality == Modality.FINAL
internal val IrFunction.isOpen get() = this is IrSimpleFunction && this.modality == Modality.OPEN
internal val IrFunction.isAbstract get() = this is IrSimpleFunction && this.modality == Modality.ABSTRACT
