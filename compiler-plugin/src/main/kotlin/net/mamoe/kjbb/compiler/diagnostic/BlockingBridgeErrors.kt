package net.mamoe.kjbb.compiler.diagnostic

//import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity.WARNING
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0.create as create0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1.create as create1

object BlockingBridgeErrors {
    // val INLINE_CLASSES_NOT_SUPPORTED: DiagnosticFactory0<PsiElement> = create0(ERROR)

    val PLUGIN_IS_NOT_ENABLED: DiagnosticFactory0<PsiElement> = create0(WARNING)

    val OVERRIDING_GENERATED_BLOCKING_BRIDGE: DiagnosticFactory1<PsiElement, String> = create1(WARNING)


    init {
        Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(
            BlockingBridgeErrors::class.java,
            BlockingBridgeErrorsRendering
        )
    }
}