package net.mamoe.kjbb.compiler.backend.jvm

import net.mamoe.kjbb.compiler.backend.jvm.BlockingBridgeAnalyzeResult.*
import net.mamoe.kjbb.compiler.extensions.IJvmBlockingBridgeCodegenJvmExtension
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.effectiveVisibility
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.isInlineClassType


fun FunctionDescriptor.analyzeCapabilityForGeneratingBridges(
    isIr: Boolean,
    bindingContext: BindingContext,
    ext: IJvmBlockingBridgeCodegenJvmExtension,
): BlockingBridgeAnalyzeResult {
    var annotationFromContainingClass = false

    // null iff enableForModule
    val jvmBlockingBridgeAnnotation =
        jvmBlockingBridgeAnnotation()
            ?: jvmBlockingBridgeAnnotationOnContainingDeclaration(isIr, bindingContext)
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
        if (containingClass == null) {
            // top-level only supported by IR
            if (!isIr) {
                return TopLevelFunctionsNotSupported(jvmBlockingBridgeAnnotationPsi ?: return EnableForModule)
            }
        }
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
                it.analyzeCapabilityForGeneratingBridges(isIr, bindingContext, ext).shouldGenerate
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
