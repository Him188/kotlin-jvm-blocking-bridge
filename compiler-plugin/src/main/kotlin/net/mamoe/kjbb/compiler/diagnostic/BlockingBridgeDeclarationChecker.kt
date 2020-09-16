package net.mamoe.kjbb.compiler.diagnostic

import net.mamoe.kjbb.compiler.backend.jvm.analyzeCapabilityForGeneratingBridges
import net.mamoe.kjbb.compiler.backend.jvm.hasJvmBlockingBridgeAnnotation
import net.mamoe.kjbb.compiler.backend.jvm.jvmBlockingBridgeAnnotation
import net.mamoe.kjbb.compiler.backend.jvm.report
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeDeclarationChecker.CheckResult.BREAK
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeDeclarationChecker.CheckResult.CONTINUE
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeErrors.INAPPLICABLE_JVM_BLOCKING_BRIDGE
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

open class BlockingBridgeDeclarationChecker(
    private val isIr: Boolean,
) : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        when (BREAK) {
            checkIsPluginEnabled(declaration, descriptor, context),
            checkApplicability(declaration, descriptor, context),
            -> return
            else -> return
        }
    }

    enum class CheckResult {
        CONTINUE,
        BREAK
    }

    protected open fun checkIsPluginEnabled(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ): CheckResult {
        return CONTINUE
    }

    private fun checkApplicability(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ): CheckResult {
        if (!descriptor.hasJvmBlockingBridgeAnnotation()) return CONTINUE
        if (descriptor !is FunctionDescriptor) {
            val inspectionTarget = descriptor.jvmBlockingBridgeAnnotation() ?: declaration
            context.report(INAPPLICABLE_JVM_BLOCKING_BRIDGE.on(inspectionTarget))
            return BREAK
        }

        val result = descriptor.analyzeCapabilityForGeneratingBridges(isIr)
        result.createDiagnostic()?.let(context::report)
        return CONTINUE
    }
}
