package net.mamoe.kjbb.compiler.backend.jvm

import com.intellij.psi.PsiElement
import net.mamoe.kjbb.compiler.backend.ir.JVM_BLOCKING_BRIDGE_FQ_NAME
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeErrors.*
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPrivateApi
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.source.getPsi


sealed class BlockingBridgeAnalyzeResult(
    val diagnosticPassed: Boolean,
    val shouldGenerate: Boolean = diagnosticPassed,
) {
    open fun createDiagnostic(): Diagnostic? = null

    object ALLOWED : BlockingBridgeAnalyzeResult(true)
    object MISSING_ANNOTATION_PSI : BlockingBridgeAnalyzeResult(true, false)
    object FROM_STUB : BlockingBridgeAnalyzeResult(true, false)
    class Inapplicable(
        private val inspectionTarget: PsiElement,
    ) : BlockingBridgeAnalyzeResult(false) {
        override fun createDiagnostic(): Diagnostic = INAPPLICABLE_JVM_BLOCKING_BRIDGE.on(inspectionTarget)
    }

    /**
     * When super member has `@JvmBlockingBridge`, but this doesn't.
     */
    class OriginFunctionOverridesSuperMember(
        val overridingMethod: FunctionDescriptor,
        shouldGenerate: Boolean,
    ) : BlockingBridgeAnalyzeResult(false, shouldGenerate)

    /**
     * Below Java 8
     */
    class InterfaceNotSupported(
        private val inspectionTarget: PsiElement,
    ) : BlockingBridgeAnalyzeResult(false) {
        override fun createDiagnostic(): Diagnostic = INTERFACE_NOT_SUPPORTED.on(inspectionTarget)
    }

    class InlineClassesNotSupported(
        /**
         * [ParameterDescriptor], [ClassDescriptor]
         */
        private val inspectionTarget: PsiElement,
        private val causeDeclaration: DeclarationDescriptor,
    ) : BlockingBridgeAnalyzeResult(false) {
        override fun createDiagnostic(): Diagnostic =
            INLINE_CLASSES_NOT_SUPPORTED.on(inspectionTarget, causeDeclaration)
    }

    class RedundantForPrivateDeclarations(
        private val inspectionTarget: PsiElement,
    ) : BlockingBridgeAnalyzeResult(false, false) {
        override fun createDiagnostic(): Diagnostic =
            REDUNDANT_JVM_BLOCKING_BRIDGE_ON_PRIVATE_DECLARATIONS.on(inspectionTarget)
    }

    /**
     * With JVM backend
     */
    class TopLevelFunctionsNotSupported(
        private val inspectionTarget: PsiElement,
    ) : BlockingBridgeAnalyzeResult(false, false) {
        override fun createDiagnostic(): Diagnostic = TOP_LEVEL_FUNCTIONS_NOT_SUPPORTED.on(inspectionTarget)
    }
}

internal fun TargetPlatform.isJvm8OrHigher(): Boolean {
    return componentPlatforms
        .any { (it.targetPlatformVersion as? JvmTarget)?.bytecodeVersion ?: 0 >= JvmTarget.JVM_1_8.bytecodeVersion }
}

/**
 * Simple version of [analyzeCapabilityForGeneratingBridges]
 */
fun FunctionDescriptor.shouldGenerateBlockingBridge(): Boolean {
    if (isGeneratedBlockingBridgeStub()) return false
    val containingClass = containingClass
    if (containingClass?.isInlineClass() == true) return false
    if (isEffectivelyPrivateApi) return false
    if (allParameters.any { it.type.isInlineClassType() }) return false
    if (containingClass?.kind == ClassKind.INTERFACE) return module.platform?.isJvm8OrHigher() == true

    if (!isSuspend || name.isSpecial || !annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)) return false
    val jvmBlockingBridgeAnnotation = jvmBlockingBridgeAnnotation() ?: return false

    // then check inheritance

    val psi = findPsi() ?: return true // psi is null so it's a man-made function
    if (overriddenDescriptors.none { it.shouldGenerateBlockingBridge() }) return true // so that the annotation
    return jvmBlockingBridgeAnnotation.isChildOf(psi)
}

internal fun PsiElement.isChildOf(parent: PsiElement): Boolean = this.parents.any { it == parent }

internal fun ClassDescriptor.isInterface(): Boolean = this.kind == ClassKind.INTERFACE

fun FunctionDescriptor.analyzeCapabilityForGeneratingBridges(isIr: Boolean): BlockingBridgeAnalyzeResult {
    val jvmBlockingBridgeAnnotation = jvmBlockingBridgeAnnotation()
        ?: return BlockingBridgeAnalyzeResult.MISSING_ANNOTATION_PSI

    if (isGeneratedBlockingBridgeStub()) return BlockingBridgeAnalyzeResult.FROM_STUB
    if (isEffectivelyPrivateApi) return BlockingBridgeAnalyzeResult.RedundantForPrivateDeclarations(
        jvmBlockingBridgeAnnotation)
    val containingClass = containingClass
    if (containingClass == null) {
        if (!isIr) {
            return BlockingBridgeAnalyzeResult.TopLevelFunctionsNotSupported(jvmBlockingBridgeAnnotation)
        }
    }
    if (containingClass?.isInlineClass() == true)
        return BlockingBridgeAnalyzeResult.InlineClassesNotSupported(jvmBlockingBridgeAnnotation, containingClass)
    allParameters.firstOrNull { it.type.isInlineClassType() }?.let { param ->
        return BlockingBridgeAnalyzeResult.InlineClassesNotSupported(
            param.findPsi() ?: jvmBlockingBridgeAnnotation, param)
    }

    if (containingClass?.isInterface() == true) { // null means top-level, which is also accepted
        if (module.platform?.isJvm8OrHigher() != true)
            return BlockingBridgeAnalyzeResult.InterfaceNotSupported(jvmBlockingBridgeAnnotation)
    } else {
        if (!isSuspend || name.isSpecial || !annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)) {
            return BlockingBridgeAnalyzeResult.Inapplicable(jvmBlockingBridgeAnnotation)
        }

        val overridden = original.overriddenDescriptors

        if (overridden.any { it.shouldGenerateBlockingBridge() })  // overriding a super function
            return BlockingBridgeAnalyzeResult.OriginFunctionOverridesSuperMember(overridden.first(),
                isDeclaredFunction())
    }

    return BlockingBridgeAnalyzeResult.ALLOWED
}

internal fun FunctionDescriptor.isDeclaredFunction(): Boolean = original.toSourceElement.getPsi() != null

internal val FunctionDescriptor.containingClass: ClassDescriptor?
    get() = this.parents.firstOrNull { it is ClassDescriptor } as ClassDescriptor?

internal fun DeclarationCheckerContext.report(diagnostic: Diagnostic) = trace.report(diagnostic)
internal fun DeclarationDescriptor.jvmBlockingBridgeAnnotation(): PsiElement? =
    annotations.findAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)?.findPsi()

internal fun AnnotationDescriptor.findPsi(): PsiElement? = source.getPsi()