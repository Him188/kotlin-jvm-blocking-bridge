package me.him188.kotlin.jvm.blocking.bridge.compiler.backend.resolve

import com.intellij.psi.PsiElement
import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.ir.RuntimeIntrinsics
import me.him188.kotlin.jvm.blocking.bridge.compiler.diagnostic.BlockingBridgeErrors.*
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
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

    object EnableForModule : BridgeAnnotationFromContainingDeclaration(null)

    /**
     * Has JvmBlockingBridge annotation on containing declaration, but this function is not capable to have bridge.
     *
     * @since 1.8
     */
    open class BridgeAnnotationFromContainingDeclaration(
        val original: BlockingBridgeAnalyzeResult?,
    ) : BlockingBridgeAnalyzeResult(true, false)

    class Inapplicable(
        private val inspectionTarget: PsiElement,
    ) : BlockingBridgeAnalyzeResult(false) {
        override fun createDiagnostic(): Diagnostic = INAPPLICABLE_JVM_BLOCKING_BRIDGE.on(inspectionTarget)
    }

    /**
     * When super member has `@JvmBlockingBridge`, but this doesn't.
     */
    class OverridesSuper(shouldGenerate: Boolean) : BlockingBridgeAnalyzeResult(true, shouldGenerate)

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

fun TargetPlatform.isJvm8OrHigher(): Boolean {
    return componentPlatforms
        .any { it.targetPlatformVersion as? JvmTarget ?: JvmTarget.DEFAULT >= JvmTarget.JVM_1_8 }
}

fun TargetPlatform.hasJvmComponent(): Boolean {
    return componentPlatforms
        .any { it.targetPlatformVersion is JvmTarget }
}

internal fun ClassDescriptor.isInterface(): Boolean = this.kind == ClassKind.INTERFACE

fun FunctionDescriptor.jvmBlockingBridgeAnnotationOnContainingDeclaration(
    isIr: Boolean,
    bindingContext: BindingContext,
): AnnotationDescriptor? {
    return when (val containingDeclaration = containingDeclaration) {
        is ClassDescriptor -> {
            // member function

            val ann = containingDeclaration.annotations.findAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName)
            if (ann != null) return ann

            containingDeclaration.findFileAnnotation(bindingContext, RuntimeIntrinsics.JvmBlockingBridgeFqName)
        }
        is PackageFragmentDescriptor -> {
            // top-level function
            if (isIr) return containingDeclaration.jvmBlockingBridgeAnnotation()
            containingDeclaration.annotations.findAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName)
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

internal fun FunctionDescriptor.isUserDeclaredFunction(): Boolean = original.toSourceElement.getPsi() != null

internal val FunctionDescriptor.containingClass: ClassDescriptor?
    get() = this.parents.firstOrNull { it is ClassDescriptor } as ClassDescriptor?

internal fun DeclarationCheckerContext.report(diagnostic: Diagnostic) = trace.report(diagnostic)
internal fun DeclarationDescriptor.jvmBlockingBridgeAnnotationPsi(): PsiElement? =
    annotations.findAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName)?.findPsi()

internal fun DeclarationDescriptor.jvmBlockingBridgeAnnotation(): AnnotationDescriptor? =
    annotations.findAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName)

internal fun AnnotationDescriptor.findPsi(): PsiElement? = (source as? PsiSourceElement)?.psi