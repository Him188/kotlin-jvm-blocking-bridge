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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.resolve.source.getPsi


sealed class BlockingBridgeAnalyzeResult(
    val diagnosticPassed: Boolean,
    val shouldGenerate: Boolean = diagnosticPassed,
) {
    open fun createDiagnostic(): Diagnostic? = null

    object Allowed : BlockingBridgeAnalyzeResult(true)
    object MissingAnnotationPsi : BlockingBridgeAnalyzeResult(true, false)
    object FromStub : BlockingBridgeAnalyzeResult(true, false)

    /**
     * Has JvmBlockingBridge annotation on containing declaration, but this function is not capable to have bridge.
     *
     * @since 1.8
     */
    class BridgeAnnotationFromContainingDeclaration(
        val original: BlockingBridgeAnalyzeResult,
    ) : BlockingBridgeAnalyzeResult(true, false)

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

    class RedundantForNonPublicDeclarations(
        private val inspectionTarget: PsiElement,
    ) : BlockingBridgeAnalyzeResult(false, false) {
        override fun createDiagnostic(): Diagnostic =
            REDUNDANT_JVM_BLOCKING_BRIDGE_ON_NON_PUBLIC_DECLARATIONS.on(inspectionTarget)
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
        .any { it.targetPlatformVersion as? JvmTarget ?: JvmTarget.DEFAULT >= JvmTarget.JVM_1_8 }
}

internal fun PsiElement.isChildOf(parent: PsiElement): Boolean = this.parents.any { it == parent }

internal fun ClassDescriptor.isInterface(): Boolean = this.kind == ClassKind.INTERFACE

fun FunctionDescriptor.jvmBlockingBridgeAnnotationOnContainingDeclaration(
    isIr: Boolean,
    bindingContext: BindingContext,
): AnnotationDescriptor? {
    return when (val containingDeclaration = containingDeclaration) {
        is ClassDescriptor -> {
            // member function

            val ann = containingDeclaration.annotations.findAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)
            if (ann != null) return ann

            containingDeclaration.findFileAnnotation(bindingContext, JVM_BLOCKING_BRIDGE_FQ_NAME)
        }
        is PackageFragmentDescriptor -> {
            // top-level function
            if (isIr) return containingDeclaration.jvmBlockingBridgeAnnotation()
            containingDeclaration.annotations.findAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)
        }
        else -> return null
    }
}

fun DeclarationDescriptorWithSource.findFileAnnotation(
    bindingContext: BindingContext,
    fqName: FqName,
): AnnotationDescriptor? {
    return containingFileAnnotations(bindingContext)?.find { it.fqName == fqName }
}

// ((((this.containingDeclaration as LazyClassDescriptor).source as KotlinSourceElement).containingFile as PsiSourceFile).psiFile as KtFile).annotationEntries
fun DeclarationDescriptorWithSource.containingFileAnnotations(bindingContext: BindingContext): List<AnnotationDescriptor>? {
    val sourceFile = (source as? KotlinSourceElement)?.containingFile as? PsiSourceFile ?: return null
    val file = sourceFile.psiFile as? KtFile ?: return null
    return file.annotationEntries.mapNotNull {
        bindingContext[BindingContext.ANNOTATION, it]
    }
}

fun FunctionDescriptor.analyzeCapabilityForGeneratingBridges(
    isIr: Boolean,
    bindingContext: BindingContext,
): BlockingBridgeAnalyzeResult {
    var annotationFromContainingClass = false

    val jvmBlockingBridgeAnnotation =
        jvmBlockingBridgeAnnotation()
            ?: jvmBlockingBridgeAnnotationOnContainingDeclaration(isIr,
                bindingContext).also { annotationFromContainingClass = true }
            ?: return BlockingBridgeAnalyzeResult.MissingAnnotationPsi

    val jvmBlockingBridgeAnnotationPsi: PsiElement =
        jvmBlockingBridgeAnnotation.findPsi()
            ?: return BlockingBridgeAnalyzeResult.MissingAnnotationPsi

    // now that the function has @JvmBlockingBridge on self or containing declaration

    fun impl(): BlockingBridgeAnalyzeResult {
        if (isGeneratedBlockingBridgeStub()) return BlockingBridgeAnalyzeResult.FromStub
        val containingClass = containingClass
        if (containingClass == null) {
            if (!isIr) {
                return BlockingBridgeAnalyzeResult.TopLevelFunctionsNotSupported(jvmBlockingBridgeAnnotationPsi)
            }
        }
        if (!visibility.effectiveVisibility(this, true).publicApi)
            return BlockingBridgeAnalyzeResult.RedundantForNonPublicDeclarations(jvmBlockingBridgeAnnotationPsi)
        if (containingClass?.isInlineClass() == true)
            return BlockingBridgeAnalyzeResult.InlineClassesNotSupported(jvmBlockingBridgeAnnotationPsi,
                containingClass)
        allParameters.firstOrNull { it.type.isInlineClassType() }?.let { param ->
            return BlockingBridgeAnalyzeResult.InlineClassesNotSupported(
                param.findPsi() ?: jvmBlockingBridgeAnnotationPsi, param)
        }

        if (containingClass?.isInterface() == true) { // null means top-level, which is also accepted
            if (module.platform?.isJvm8OrHigher() != true)
                return BlockingBridgeAnalyzeResult.InterfaceNotSupported(jvmBlockingBridgeAnnotationPsi)
        } else {
            if (!isSuspend || name.isSpecial) {
                return BlockingBridgeAnalyzeResult.Inapplicable(jvmBlockingBridgeAnnotationPsi)
            }

            val overridden = original.overriddenDescriptors
            val shouldGen =
                overridden.find { it.analyzeCapabilityForGeneratingBridges(isIr, bindingContext).shouldGenerate }
            if (shouldGen != null)  // overriding a super function
                return BlockingBridgeAnalyzeResult.OriginFunctionOverridesSuperMember(shouldGen, isDeclaredFunction())
        }

        return BlockingBridgeAnalyzeResult.Allowed
    }

    val result = impl()
    if (annotationFromContainingClass) {
        if (!result.diagnosticPassed) {
            return BlockingBridgeAnalyzeResult.BridgeAnnotationFromContainingDeclaration(result)
        }
    }
    return result
}

internal fun FunctionDescriptor.isDeclaredFunction(): Boolean = original.toSourceElement.getPsi() != null

internal val FunctionDescriptor.containingClass: ClassDescriptor?
    get() = this.parents.firstOrNull { it is ClassDescriptor } as ClassDescriptor?

internal fun DeclarationCheckerContext.report(diagnostic: Diagnostic) = trace.report(diagnostic)
internal fun DeclarationDescriptor.jvmBlockingBridgeAnnotationPsi(): PsiElement? =
    annotations.findAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)?.findPsi()

internal fun DeclarationDescriptor.jvmBlockingBridgeAnnotation(): AnnotationDescriptor? =
    annotations.findAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)

internal fun AnnotationDescriptor.findPsi(): PsiElement? = source.getPsi()