package net.mamoe.kjbb.ide

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class BlockingBridgeLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return null
    }

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        val markedLineNumbers = HashSet<Int>()

        for (element in elements) {
            ProgressManager.checkCanceled()

            if (element !is PsiReferenceExpression) continue

            val containingFile = element.containingFile
            if (containingFile !is PsiJavaFile || containingFile is PsiJavaCodeReferenceCodeFragment) {
                continue
            }

            val lineNumber = element.getLineNumber()
            if (lineNumber in markedLineNumbers) continue
            if (!element.hasBridgeCalls()) continue


            markedLineNumbers += lineNumber
            result += if (element is KtForExpression) {
                BridgeCallLineMarkerInfo(
                    getElementForLineMark(element.loopRange!!),
                    // KotlinBundle.message("highlighter.message.suspending.iteration")
                )
            } else {
                BridgeCallLineMarkerInfo(
                    getElementForLineMark(element),
                    //KotlinBundle.message("highlighter.message.suspend.function.call")
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    class BridgeCallLineMarkerInfo(
        callElement: PsiElement
    ) : LineMarkerInfo<PsiElement>(
        callElement,
        callElement.textRange,
        Icons.BridgedSuspendCall,
        Pass.LINE_MARKERS,
        {
            "Blocking bridge method call"
        },
        null,
        GutterIconRenderer.Alignment.RIGHT
    ) {
        override fun createGutterRenderer(): GutterIconRenderer? {
            return object : LineMarkerInfo.LineMarkerGutterIconRenderer<PsiElement>(this) {
                override fun getClickAction(): AnAction? = null
            }
        }
    }

}

fun PsiReferenceExpression.hasBridgeCalls(): Boolean {
    val resolved = this.resolve() as? KtLightMethod ?: return false

    return resolved.canHaveBridgeFunctions()
}

fun PsiElement.getLineNumber(start: Boolean = true): Int {
    val document =
        containingFile.viewProvider.document ?: PsiDocumentManager.getInstance(project).getDocument(containingFile)
    val index = if (start) this.startOffset else this.endOffset
    if (index > document?.textLength ?: 0) return 0
    return document?.getLineNumber(index) ?: 0
}

internal fun getElementForLineMark(callElement: PsiElement): PsiElement =
    when (callElement) {
        is KtSimpleNameExpression -> callElement.getReferencedNameElement()
        else ->
            // a fallback,
            //but who knows what to reference in KtArrayAccessExpression ?
            generateSequence(callElement, { it.firstChild }).last()
    }