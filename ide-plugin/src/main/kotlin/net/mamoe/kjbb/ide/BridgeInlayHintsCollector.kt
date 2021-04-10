@file:Suppress("UnstableApiUsage")

package net.mamoe.kjbb.ide

import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiExtensibleClass
import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.kjbb.compiler.backend.ir.JVM_BLOCKING_BRIDGE_FQ_NAME
import net.mamoe.kjbb.ide.line.marker.document
import net.mamoe.kjbb.ide.line.marker.getLineNumber
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.codeInsight.hints.HintType
import org.jetbrains.kotlin.idea.codeInsight.hints.KotlinAbstractHintsProvider
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.types.KotlinType
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel

internal val PsiElement.containingKtFile: KtFile?
    get() = (containingFile as? FakeFileForLightClass)?.ktFile
        ?: (this as? KtLightDeclaration<*, *>)?.kotlinOrigin?.containingKtFile

internal val PsiMember.containingKtClass: KtClassOrObject?
    get() = (containingClass as? KtLightClass)?.kotlinOrigin

class BridgeInlayHintsCollector :
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

        if (!element.isBridgeCompilerEnabled) return false

        val isIr = element.isIr
        for (method in element.methods) {
            if (method is BlockingBridgeStubMethod) continue
            if (method.containingClass !== element) continue
            if (method.canHaveBridgeFunctions(isIr).inlayHints) {
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


    private fun createPresentation(
        factory: PresentationFactory,
        method: PsiMethod,
        editor: Editor,
    ): InlayPresentation? {
        var hint =
            factory.text("@${JvmBlockingBridge::class.simpleName}")

        fun createNavigation(mouseEvent: MouseEvent, target: NavigatablePsiElement) {
            PsiElementListNavigator.openTargets(mouseEvent, arrayOf(target),
                "Navigate To Annotation Source",
                "Find Navigation Target",
                DefaultPsiElementCellRenderer())
        }

        var annotation: KtAnnotationEntry?
        if (method !is KtLightMethod) return null
        if (method.lightMemberOrigin?.originKind != JvmDeclarationOriginKind.OTHER) return null
        if (method.isJvmStatic() && method.containingKtClass !is KtObjectDeclaration) return null

        when {
            method.containingKtClass?.findAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)
                .also { annotation = it } != null -> {

                val containingClass = method.containingClass
                hint = factory.withTooltip(
                    "From @JvmBlockingBridge on class ${containingClass.name}",
                    hint
                )
                hint = factory.onClick(hint, MouseButton.Middle) { mouseEvent, _ ->
                    createNavigation(
                        mouseEvent,
                        annotation!!
                    )
                }
            }
            method.containingKtFile?.findAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)
                .also { annotation = it } != null -> {

                val containingKtFile = method.containingKtFile!!
                hint = factory.withTooltip(
                    "From @file:JvmBlockingBridge on file ${containingKtFile.name}",
                    hint
                )
                hint = factory.onClick(hint, MouseButton.Middle) { mouseEvent, _ ->
                    createNavigation(
                        mouseEvent,
                        annotation!!
                    )
                }
            }
            else -> {
                hint = factory.withTooltip(
                    "From enableForModule",
                    hint
                )
            }
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