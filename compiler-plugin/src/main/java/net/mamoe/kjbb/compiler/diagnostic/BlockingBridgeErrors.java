package net.mamoe.kjbb.compiler.diagnostic;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.diagnostics.Errors;

import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;
import static org.jetbrains.kotlin.diagnostics.Severity.WARNING;


public interface BlockingBridgeErrors {
    // DiagnosticFactory0<PsiElement> PLUGIN_IS_NOT_ENABLED = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> INAPPLICABLE_JVM_BLOCKING_BRIDGE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, DeclarationDescriptor> INLINE_CLASSES_NOT_SUPPORTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> INTERFACE_NOT_SUPPORTED_BELOW_JAVA8 = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> REDUNDANT_JVM_BLOCKING_BRIDGE_ON_PRIVATE_DECLARATIONS = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory1<PsiElement, String> OVERRIDING_GENERATED_BLOCKING_BRIDGE = DiagnosticFactory1.create(WARNING);

    @Deprecated
    Object _init = new Object() {
        {
            Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(
                    BlockingBridgeErrors.class,
                    BlockingBridgeErrorsRendering.INSTANCE
            );
        }
    };
}
