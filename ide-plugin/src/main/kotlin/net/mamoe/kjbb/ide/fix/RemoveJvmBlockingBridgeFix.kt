package net.mamoe.kjbb.ide.fix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentsOfType
import net.mamoe.kjbb.compiler.backend.ir.JVM_BLOCKING_BRIDGE_FQ_NAME
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.inspections.KotlinUniversalQuickFix
import org.jetbrains.kotlin.idea.quickfix.KotlinCrossLanguageQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class RemoveJvmBlockingBridgeFix(
    element: KtFunction,
) : KotlinCrossLanguageQuickFixAction<KtModifierListOwner>(element), KotlinUniversalQuickFix {

    override fun getFamilyName(): String = BlockingBridgeBundle.message("remove.jvm.blocking.bridge.fix")
    override fun getText(): String = BlockingBridgeBundle.message("remove.jvm.blocking.bridge")

    override fun invokeImpl(project: Project, editor: Editor?, file: PsiFile) {
        element?.findAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)?.delete() ?: return
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val target = diagnostic.psiElement.safeAs<KtAnnotationEntry>()?.parentsOfType<KtFunction>()?.firstOrNull()
                ?: return null
            return RemoveJvmBlockingBridgeFix(target)
        }

        override fun isApplicableForCodeFragment(): Boolean = false
    }
}
