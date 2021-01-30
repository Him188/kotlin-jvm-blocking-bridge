package net.mamoe.kjbb.compiler.diagnostic

import net.mamoe.kjbb.compiler.backend.jvm.*
import net.mamoe.kjbb.compiler.backend.jvm.HasJvmBlockingBridgeAnnotation.*
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeDeclarationChecker.CheckResult.BREAK
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeDeclarationChecker.CheckResult.CONTINUE
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeErrors.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmSyntheticAnnotation

open class BlockingBridgeDeclarationChecker(
    private val isIr: Boolean,
) : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        if (declaration !is KtNamedFunction) return // @JvmBlockingBridge is only applicable to CLASS and FUNCTION, no need to check for CLASS

        when (BREAK) {
            checkApplicability(declaration, descriptor, context),
            checkJvmSynthetic(declaration, descriptor, context),
            -> return
            else -> return
        }
    }

    enum class CheckResult {
        CONTINUE,
        BREAK
    }

    protected open fun checkIsPluginEnabled(
        descriptor: DeclarationDescriptor,
    ): Boolean {
        return true // in CLI compiler, always enabled
    }

    private fun checkApplicability(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ): CheckResult {
        val inspectionTarget = when (descriptor.hasJvmBlockingBridgeAnnotation()) {
            NONE -> return CONTINUE
            FROM_CONTAINING_DECLARATION -> return CONTINUE // no need to check applicability for inherited from containing class or file
            FROM_FUNCTION -> descriptor.jvmBlockingBridgeAnnotationPsi() ?: declaration
        }

        if (!checkIsPluginEnabled(descriptor)) {
            context.report(BLOCKING_BRIDGE_PLUGIN_NOT_ENABLED.on(inspectionTarget))
            return BREAK
        }

        if (descriptor !is FunctionDescriptor) {
            context.report(INAPPLICABLE_JVM_BLOCKING_BRIDGE.on(inspectionTarget))
            return BREAK
        }

        val result = descriptor.analyzeCapabilityForGeneratingBridges(isIr, context.trace.bindingContext)
        result.createDiagnostic()?.let(context::report)
        return CONTINUE
    }

    companion object {

        private val JVM_SYNTHETIC = FqName("kotlin.jvm.JvmSynthetic")
    }

    private fun checkJvmSynthetic(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ): CheckResult {
        val inspectionTarget = when (descriptor.hasJvmBlockingBridgeAnnotation()) {
            NONE -> return CONTINUE
            FROM_CONTAINING_DECLARATION, FROM_FUNCTION -> descriptor.jvmBlockingBridgeAnnotationPsi()
                ?: descriptor.annotations.findAnnotation(JVM_SYNTHETIC)?.findPsi()
                ?: declaration
        }

        if (descriptor.hasJvmSyntheticAnnotation()) {
            context.report(REDUNDANT_JVM_BLOCKING_BRIDGE_WITH_JVM_SYNTHETIC.on(inspectionTarget))
            return CONTINUE
        }
        return CONTINUE
    }
}
