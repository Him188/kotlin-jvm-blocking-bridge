package net.mamoe.kjbb.ide

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.util.castSafelyTo
import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.kjbb.compiler.backend.ir.JVM_BLOCKING_BRIDGE_FQ_NAME
import net.mamoe.kjbb.compiler.backend.jvm.HasJvmBlockingBridgeAnnotation
import net.mamoe.kjbb.ide.line.marker.document
import net.mamoe.kjbb.ide.line.marker.getLineNumber
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClassForFacade
import org.jetbrains.kotlin.idea.caches.project.toDescriptor
import org.jetbrains.kotlin.idea.codeInsight.hints.HintType
import org.jetbrains.kotlin.idea.codeInsight.hints.KotlinAbstractHintsProvider
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.KotlinType
import javax.swing.JComponent
import javax.swing.JPanel

class JvmBlockingBridgeInlayHintsCollector :
    InlayHintsProvider<NoSettings>,
    KotlinAbstractHintsProvider<NoSettings>(),
    InlayHintsCollector {

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean = kotlin.runCatching {
        // wrapped with runCatching in case binary changes. it's better not to provide feature than throwing exceptions

        if (element is KtFile) {
            var anyChanged = false

            fun collectClass(clazz: PsiClass): Boolean {
                anyChanged = collect(clazz, editor, sink) || anyChanged
                for (inner in clazz.innerClasses) {
                    anyChanged = collectClass(inner) || anyChanged
                }
                return anyChanged
            }

            for (clazz in element.classes) {
                collectClass(clazz)
            }
            return anyChanged
        }

        if (element !is KtUltraLightClass && element !is KtUltraLightClassForFacade) return false
        if (element !is PsiExtensibleClass) return false

        if (editor !is EditorImpl) return false

        var anyChanged = false
        val factory = PresentationFactory(editor)

        val isIr = element.module?.toDescriptor()?.isIr() == true
        for (method in element.methods) {
            if (method is BlockingBridgeStubMethod) continue
            if (method.containingClass !== element) continue
            if (method.canHaveBridgeFunctions(isIr) == HasJvmBlockingBridgeAnnotation.FROM_CONTAINING_DECLARATION) {
                anyChanged = true
                sink.addBlockElement(
                    offset = method.identifyingElement?.startOffset ?: method.startOffset,
                    relatesToPrecedingText = false,
                    showAbove = true,
                    priority = 1,
                    presentation = createPresentation(factory, method, editor) ?: continue,
                )
            }
        }

        return anyChanged
    }.getOrElse { false }

    private fun KtTypeReference.resolveType(): KotlinType? {
        return (this.typeElement as? KtUserType)?.referenceExpression?.resolveType()
    }

    private fun KtAnnotated.hasAnnotation(fqName: FqName): Boolean {
        return this.findAnnotation(fqName) != null
    }

    private fun createPresentation(
        factory: PresentationFactory,
        method: PsiMethod,
        editor: Editor,
    ): InlayPresentation? {
        var hint =
            factory.text("@${JvmBlockingBridge::class.simpleName}")

        hint = factory.onClick(hint, MouseButton.Middle) { mouseEvent, point ->
            // TODO: 2021/1/29 navigate to annotation on containing declaration
        }

        if (method.containingClass?.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME.asString()) == true) {
            hint = factory.withTooltip(
                "From @JvmBlockingBridge on class ${method.containingClass?.name}",
                hint
            )
        } else if (method.containingFile.castSafelyTo<KtFile>()?.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME) == true) {
            hint = factory.withTooltip(
                "From @file:JvmBlockingBridge on file ${method.containingFile.name}",
                hint
            )
        }

        val alignmentElement = method.modifierList
        val lineStart =
            method.document?.getLineStartOffset((alignmentElement).getLineNumber()) ?: return null

        hint = factory.inset(
            hint,
            left = ((alignmentElement.startOffset - lineStart) * EditorUtil.getPlainSpaceWidth(editor))
        )
//        factory.seq(
//            hint,
//            factory.textSpacePlaceholder((method.modifierList.startOffsetSkippingComments - lineStart - 3).coerceAtLeast(
//                0), true)
//        )

        return hint
    }

    override val name: String get() = "JvmBlockingBridge hints"

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = JPanel()
        }
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink,
    ): InlayHintsCollector? {
        if (file !is KtFile) return null
        return this
    }

    override fun createSettings(): NoSettings {
        return NoSettings()
    }

    override fun isElementSupported(resolved: HintType?, settings: NoSettings): Boolean {
        return resolved == HintType.FUNCTION_HINT
    }
}