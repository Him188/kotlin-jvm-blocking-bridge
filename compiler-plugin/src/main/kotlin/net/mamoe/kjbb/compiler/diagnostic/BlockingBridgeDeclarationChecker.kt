package net.mamoe.kjbb.compiler.diagnostic

import com.intellij.psi.PsiElement
import net.mamoe.kjbb.compiler.backend.ir.JVM_BLOCKING_BRIDGE_FQ_NAME
import net.mamoe.kjbb.compiler.backend.jvm.BlockingBridgeTestResult
import net.mamoe.kjbb.compiler.backend.jvm.canGenerateJvmBlockingBridge
import net.mamoe.kjbb.compiler.backend.jvm.hasJvmBlockingBridgeAnnotation
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPrivateApi
import org.jetbrains.kotlin.resolve.descriptorUtil.isInsidePrivateClass
import org.jetbrains.kotlin.resolve.source.getPsi

open class BlockingBridgeDeclarationChecker : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        when {
            checkIsPluginEnabled(declaration, descriptor, context) -> return
            checkApplicability(declaration, descriptor, context) -> return
        }
    }

    protected open fun checkIsPluginEnabled(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ): Boolean {
        return false
    }

    private fun checkApplicability(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ): Boolean {
        if (!descriptor.hasJvmBlockingBridgeAnnotation()) return false
        if (descriptor !is FunctionDescriptor) {
            context.report(BlockingBridgeErrors.INAPPLICABLE_JVM_BLOCKING_BRIDGE.on(descriptor.jvmBlockingBridgeAnnotation()!!))
            return true
        }

        when (val result = descriptor.canGenerateJvmBlockingBridge()) {
            BlockingBridgeTestResult.ALLOWED,
            BlockingBridgeTestResult.FROM_STUB,
            is BlockingBridgeTestResult.OriginFunctionOverridesSuperMember,
            -> {
                // no-op
            }
            BlockingBridgeTestResult.INAPPLICABLE -> {
                context.report(BlockingBridgeErrors.INAPPLICABLE_JVM_BLOCKING_BRIDGE.on(descriptor.jvmBlockingBridgeAnnotation()!!))
            }
            is BlockingBridgeTestResult.BlockingBridgeHidesSuperMember -> {
                context.report(
                    BlockingBridgeErrors.IMPLICIT_OVERRIDE_BY_JVM_BLOCKING_BRIDGE.on(
                        descriptor.jvmBlockingBridgeAnnotation()!!,
                        result.overridingMethod as KtNamedDeclaration,
                        result.overridingMethod.containingClassOrObject?.name
                            ?: result.overridingMethod.containingKtFile.name
                    )
                )
            }
        }
        if (descriptor.isEffectivelyPrivateApi || descriptor.isInsidePrivateClass) {
            context.report(BlockingBridgeErrors.REDUNDANT_JVM_BLOCKING_BRIDGE_ON_PRIVATE_DECLARATIONS.on(descriptor.jvmBlockingBridgeAnnotation()!!))
            return true
        }
        return false
    }
}

internal fun DeclarationCheckerContext.report(diagnostic: Diagnostic) = trace.report(diagnostic)
internal fun DeclarationDescriptor.jvmBlockingBridgeAnnotation(): PsiElement? =
    annotations.findAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)?.source?.getPsi()