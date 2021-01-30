package net.mamoe.kjbb.ide.fix

import com.intellij.codeInsight.intention.IntentionAction
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeErrors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.idea.quickfix.KotlinIntentionActionsFactory
import org.jetbrains.kotlin.idea.quickfix.QuickFixContributor
import org.jetbrains.kotlin.idea.quickfix.QuickFixes

class QuickFixRegistrar : QuickFixContributor {
    override fun registerQuickFixes(quickFixes: QuickFixes) {
        fun DiagnosticFactory<*>.registerFactory(vararg factory: KotlinIntentionActionsFactory) {
            quickFixes.register(this, *factory)
        }

        @Suppress("unused")
        fun DiagnosticFactory<*>.registerActions(vararg action: IntentionAction) {
            quickFixes.register(this, *action)
        }

        REDUNDANT_JVM_BLOCKING_BRIDGE_WITH_JVM_SYNTHETIC
            .registerFactory(RemoveJvmBlockingBridgeFix, RemoveJvmSyntheticFix)

        REDUNDANT_JVM_BLOCKING_BRIDGE_ON_PRIVATE_DECLARATIONS.registerFactory(RemoveJvmBlockingBridgeFix)

        INAPPLICABLE_JVM_BLOCKING_BRIDGE.registerFactory(RemoveJvmBlockingBridgeFix)
        INLINE_CLASSES_NOT_SUPPORTED.registerFactory(RemoveJvmBlockingBridgeFix)
        INTERFACE_NOT_SUPPORTED.registerFactory(RemoveJvmBlockingBridgeFix)
    }
}
