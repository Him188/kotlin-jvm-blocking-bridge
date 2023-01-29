package me.him188.kotlin.jvm.blocking.bridge.ide.fix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.inspections.KotlinUniversalQuickFix
import org.jetbrains.kotlin.idea.quickfix.KotlinCrossLanguageQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_SYNTHETIC_ANNOTATION_FQ_NAME

class RemoveJvmSyntheticFix(
    element: KtFunction,
) : KotlinCrossLanguageQuickFixAction<KtModifierListOwner>(element), KotlinUniversalQuickFix {

    override fun getFamilyName(): String = BlockingBridgeBundle.message("remove.jvm.synthetic.fix")
    override fun getText(): String = BlockingBridgeBundle.message("remove.jvm.synthetic")

    override fun invokeImpl(project: Project, editor: Editor?, file: PsiFile) {
        element?.findAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)?.delete() ?: return
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val target =
                (diagnostic.psiElement as? KtAnnotationEntry)?.parentsOfType<KtFunction>()
                    ?.firstOrNull()
                    ?: return null
            return RemoveJvmSyntheticFix(target)
        }

        override fun isApplicableForCodeFragment(): Boolean = false
    }
}
