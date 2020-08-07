package net.mamoe.kjbb.compiler.diagnostic

import net.mamoe.kjbb.compiler.backend.jvm.canGenerateJvmBlockingBridge
import net.mamoe.kjbb.compiler.backend.jvm.hasJvmBlockingBridgeAnnotation
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

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
            context.report(BlockingBridgeErrors.INAPPLICABLE_JVM_BLOCKING_BRIDGE.on(declaration))
            return true
        }
        if (!descriptor.canGenerateJvmBlockingBridge()) {
            context.report(BlockingBridgeErrors.INAPPLICABLE_JVM_BLOCKING_BRIDGE.on(declaration))
            return true
        }
        return false
    }
}

internal fun DeclarationCheckerContext.report(diagnostic: Diagnostic) = trace.report(diagnostic)