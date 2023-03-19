@file:Suppress("UnstableApiUsage")

package me.him188.kotlin.jvm.blocking.bridge.ide

import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.editor.BlockInlayPriority
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.psi.*
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.ir.RuntimeIntrinsics
import me.him188.kotlin.jvm.blocking.bridge.ide.line.marker.document
import me.him188.kotlin.jvm.blocking.bridge.ide.line.marker.getLineNumber
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel

class BridgeInlayHintsCollector :
    InlayHintsProvider<NoSettings>,
//    KotlinAbstractHintsProvider<NoSettings>(),
    InlayHintsCollector {

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean = kotlin.runCatching {
        // wrapped with runCatching in case binary changes. it's better not to provide feature than throwing exceptions

        if (element !is KtFile) return false
        if (editor !is EditorImpl) return false
        if (!element.isBridgeCompilerEnabled) return false

        var anyChanged = false
        for (clazz in element.ktClassOrObjects()) {
            anyChanged = collectInlayHintsForClass(clazz, editor, sink) || anyChanged
        }

        for (declaration in element.declarations) {
            if (declaration is KtNamedFunction) {
                anyChanged = collectInlayHintsForFunction(declaration, editor, sink) || anyChanged
            }
        }
        return anyChanged
    }.getOrElse { false }

    private fun collectInlayHintsForClass(element: KtClassOrObject, editor: EditorImpl, sink: InlayHintsSink): Boolean {
        var anyChanged = false
        element.ktClassOrObjects().forEach {
            anyChanged = collectInlayHintsForClass(it, editor, sink) || anyChanged
        }

        for (function in element.declarations.asSequence().filterIsInstance<KtFunction>()) {
            if (function.containingClass() !== element) return false
            anyChanged = collectInlayHintsForFunction(function, editor, sink) || anyChanged
        }

        return anyChanged
    }

    private fun collectInlayHintsForFunction(
        method: KtFunction,
        editor: EditorImpl,
        sink: InlayHintsSink,
    ): Boolean {
        if (method.canHaveBridgeFunctions().inlayHints) {
            val factory = PresentationFactory(editor)
            val presentation = createPresentation(factory, method, editor) ?: return false
            sink.addBlockElement(
                offset = method.identifyingElement?.startOffset ?: method.startOffset,
                relatesToPrecedingText = false,
                showAbove = true,
                priority = BlockInlayPriority.ANNOTATIONS,
                presentation = presentation,
            )
            return true
        }
        return false
    }


    private fun createPresentation(
        factory: PresentationFactory,
        method: KtFunction,
        editor: Editor,
    ): InlayPresentation? {
        var hint =
            factory.text("@${JvmBlockingBridge::class.simpleName}")

        if (method.isJvmStatic() && method.containingClassOrObject !is KtObjectDeclaration) return null

        method.containingClassOrObject?.findAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName)?.let { annotation ->
            val containingClass = method.containingClassOrObject
            hint = factory.withTooltip(
                "From ${annotation.text} on class ${containingClass?.name}",
                hint
            )
            hint = factory.withNavigation(hint, annotation)
        } ?: method.containingKtFile.findAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName)?.let { annotation ->
            val containingKtFile = method.containingKtFile
            hint = factory.withTooltip(
                "From ${annotation.text} on file ${containingKtFile.name}",
                hint
            )
            hint = factory.withNavigation(hint, annotation)
        } ?: kotlin.run {
            hint = factory.withTooltip("From enableForModule", hint)
        }

        val alignmentElement = method.modifierList ?: return null
        val lineStart =
            method.document?.getLineStartOffset(alignmentElement.getLineNumber()) ?: return null

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

    private fun PresentationFactory.withNavigation(
        base: InlayPresentation,
        target: NavigatablePsiElement,
    ): InlayPresentation {
        fun createNavigation(mouseEvent: MouseEvent) {
            PsiElementListNavigator.openTargets(
                mouseEvent, arrayOf(target),
                "Navigate To Annotation Source",
                "Find Navigation Target",
                DefaultPsiElementCellRenderer()
            )
        }

        var hint = base

        hint = onClick(hint, MouseButton.Middle) { mouseEvent, _ ->
            createNavigation(mouseEvent)
        }
        hint = onClick(hint, MouseButton.Left) { mouseEvent, _ ->
            if (!mouseEvent.isControlDown) return@onClick
            createNavigation(mouseEvent)
        }
        return hint
    }

    override val name: String get() = "JvmBlockingBridge hints"
    override val previewText: String get() = ""

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

    override val key: SettingsKey<NoSettings> get() = SettingsKey("blocking.bridge.hints")
}

private fun KtDeclarationContainer.ktClassOrObjects() =
    declarations.asSequence().filterIsInstance<KtClassOrObject>()
