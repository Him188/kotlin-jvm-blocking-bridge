package me.him188.kotlin.jvm.blocking.bridge.compiler.backend.resolve

import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.resolve.BlockingBridgeAnalyzeResult.*
import me.him188.kotlin.jvm.blocking.bridge.compiler.extensions.IBridgeConfiguration
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.effectiveVisibility
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmSyntheticAnnotation


fun FunctionDescriptor.analyzeCapabilityForGeneratingBridges(
    bindingContext: BindingContext,
    ext: IBridgeConfiguration,
): BlockingBridgeAnalyzeResult {
    var annotationFromContainingClass = false

    // null iff enableForModule
    val jvmBlockingBridgeAnnotation =
        jvmBlockingBridgeAnnotation()
            ?: jvmBlockingBridgeAnnotationOnContainingDeclaration(bindingContext)
                .also { annotationFromContainingClass = true }
            ?: kotlin.run {
                if (ext.enableForModule) {
                    null
                } else return MissingAnnotationPsi
            }

    // null iff enableForModule
    val jvmBlockingBridgeAnnotationPsi =
        if (jvmBlockingBridgeAnnotation == null) null
        else jvmBlockingBridgeAnnotation.findPsi() ?: return MissingAnnotationPsi

    val enableForModule = jvmBlockingBridgeAnnotationPsi == null

    if (enableForModule || annotationFromContainingClass) {
        if (this.hasJvmSyntheticAnnotation()) return EnableForModule
    }

    // now that the function has @JvmBlockingBridge on self or containing declaration

    fun impl(): BlockingBridgeAnalyzeResult {
        // fun must be suspend and applied to member function
        if (!isSuspend || name.isSpecial) {
            return Inapplicable(jvmBlockingBridgeAnnotationPsi ?: return EnableForModule)
        }

        if (isGeneratedBlockingBridgeStub()) {
            // @JvmBlockingBridge and @GeneratedBlockingBridge both present
            return FromStub
        }

        val containingClass = containingClass
        if (!visibility.effectiveVisibility(this, true).publicApi) {
            // effectively internal api
            return RedundantForNonPublicDeclarations(jvmBlockingBridgeAnnotationPsi ?: return EnableForModule)
        }

        if (containingClass?.isInlineClass() == true) {
            // inside inline class not supported
            return InlineClassesNotSupported(jvmBlockingBridgeAnnotationPsi ?: return EnableForModule,
                containingClass)
        }

        allParameters.firstOrNull { it.type.isInlineClassType() }?.let { param ->
            // inline class param not yet supported
            return InlineClassesNotSupported(
                param.findPsi() ?: jvmBlockingBridgeAnnotationPsi ?: return EnableForModule, param)
        }

        if (containingClass?.isInterface() == true) {
            if (module.platform?.isJvm8OrHigher() != true) {
                // inside interface and JVM under 8
                return InterfaceNotSupported(jvmBlockingBridgeAnnotationPsi ?: return EnableForModule)
            }
        }

        val overridden =
            original.findOverriddenDescriptorsHierarchically {
                it.analyzeCapabilityForGeneratingBridges(bindingContext, ext).shouldGenerate
            }

        if (overridden != null) {
            // super function has @
            // generate only if this function has @, or implied from @ on class, which concluded as 'isDeclared'
            return OverridesSuper(isUserDeclaredFunction())
        }

        // super function no @
        // this function may has @ or implied from
        return if (isUserDeclaredFunction()) {
            // explicit 'override' then generate for it.
            Allowed
        } else {
            // implicit override by compiler, don't generate.
            BridgeAnnotationFromContainingDeclaration(null)
        }
    }

    val result = impl()
    if (annotationFromContainingClass) {
        if (!result.diagnosticPassed) {
            return BridgeAnnotationFromContainingDeclaration(result)
        }
    }
    return result
}
