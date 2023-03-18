package me.him188.kotlin.jvm.blocking.bridge.compiler.diagnostic

import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.resolve.*
import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.resolve.HasJvmBlockingBridgeAnnotation.*
import me.him188.kotlin.jvm.blocking.bridge.compiler.diagnostic.BlockingBridgeDeclarationChecker.CheckResult.BREAK
import me.him188.kotlin.jvm.blocking.bridge.compiler.diagnostic.BlockingBridgeDeclarationChecker.CheckResult.CONTINUE
import me.him188.kotlin.jvm.blocking.bridge.compiler.diagnostic.BlockingBridgeErrors.*
import me.him188.kotlin.jvm.blocking.bridge.compiler.extensions.IBridgeConfiguration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmSyntheticAnnotation

open class BlockingBridgeDeclarationChecker(
    private val ext: (KtDeclaration) -> IBridgeConfiguration,
) : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        when (declaration) {
            is KtClass -> {
                val annotation = descriptor.jvmBlockingBridgeAnnotationPsi() ?: return
                if (!isPluginEnabled(descriptor)) {
                    context.report(BLOCKING_BRIDGE_PLUGIN_NOT_ENABLED.on(annotation))
                    return
                }
                if (declaration.isInterface()) {
                    if (descriptor.module.platform?.isJvm8OrHigher() != true) {
                        // below 8
                        context.report(INTERFACE_NOT_SUPPORTED.on(annotation))
                        return
                    }
                }
            }
            is KtNamedFunction -> {
                when (BREAK) {
                    checkApplicability(declaration, descriptor, context),
                    checkJvmSynthetic(declaration, descriptor, context),
                    -> return
                    else -> return
                }
            }
        }
    }

    enum class CheckResult {
        CONTINUE,
        BREAK
    }

    protected open fun isPluginEnabled(
        descriptor: DeclarationDescriptor,
    ): Boolean {
        return true // in CLI compiler, always enabled
    }

    private fun checkApplicability(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ): CheckResult = with(ext(declaration)) {
        val inspectionTarget =
            when (descriptor.hasJvmBlockingBridgeAnnotation(context.trace.bindingContext, enableForModule)) {
                NONE -> return CONTINUE

                FROM_CONTAINING_DECLARATION,
                ENABLE_FOR_MODULE,
                -> {
                    // no need to check applicability for inherited from containing class or file or enabled for module.
                    return CONTINUE
                }

                FROM_FUNCTION -> descriptor.jvmBlockingBridgeAnnotationPsi() ?: declaration
            }

        if (!isPluginEnabled(descriptor)) {
            context.report(BLOCKING_BRIDGE_PLUGIN_NOT_ENABLED.on(inspectionTarget))
            return BREAK
        }

        if (descriptor !is FunctionDescriptor) {
            context.report(INAPPLICABLE_JVM_BLOCKING_BRIDGE.on(inspectionTarget))
            return BREAK
        }

        val result = descriptor.analyzeCapabilityForGeneratingBridges(context.trace.bindingContext, this)
        result.createDiagnostic()?.let(context::report)

        if (result is BlockingBridgeAnalyzeResult.BridgeAnnotationFromContainingDeclaration) return BREAK
        return CONTINUE
    }

    companion object {

        private val JVM_SYNTHETIC = FqName("kotlin.jvm.JvmSynthetic")
    }

    private fun checkJvmSynthetic(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ): CheckResult = with(ext(declaration)) {
        val inspectionTarget =
            when (descriptor.hasJvmBlockingBridgeAnnotation(context.trace.bindingContext, enableForModule)) {
                NONE -> return CONTINUE
                FROM_FUNCTION -> descriptor.jvmBlockingBridgeAnnotationPsi()
                    ?: descriptor.annotations.findAnnotation(JVM_SYNTHETIC)?.findPsi()
                    ?: declaration
                FROM_CONTAINING_DECLARATION, ENABLE_FOR_MODULE -> return CONTINUE
            }

        if (descriptor.hasJvmSyntheticAnnotation()) {
            context.report(REDUNDANT_JVM_BLOCKING_BRIDGE_WITH_JVM_SYNTHETIC.on(inspectionTarget))
            return CONTINUE
        }
        return CONTINUE
    }
}
