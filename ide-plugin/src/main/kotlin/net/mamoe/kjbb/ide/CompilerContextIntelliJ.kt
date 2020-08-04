package net.mamoe.kjbb.ide

import com.google.auto.service.AutoService
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import net.mamoe.kjbb.compiler.context.CompilerContext
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

@AutoService(CompilerContext::class)
open class CompilerContextIntelliJ : CompilerContext {
    companion object : CompilerContextIntelliJ()

    override val kind: CompilerContext.CompilerContextKind
        get() = CompilerContext.CompilerContextKind.IntelliJ

    private object GENERATED_STUB_FOR_JAVA_RESOLVING_KEY : Key<Boolean>("kjbb.generated.key")

    override fun FunctionDescriptor.isGeneratedStubForJavaResolving(): Boolean {
        val psi = this.findPsi() as? KtLightMethod ?: return false
        return psi.getUserData(GENERATED_STUB_FOR_JAVA_RESOLVING_KEY) == true
    }

    override fun KtLightMethod.setGeneratedStubForJavaResolving() {
        this.putUserData(GENERATED_STUB_FOR_JAVA_RESOLVING_KEY, true)
    }

    override fun JvmDeclarationOrigin(
        originKind: JvmDeclarationOriginKind,
        descriptor: DeclarationDescriptor,
        parametersForJvmOverload: List<KtParameter?>?
    ): JvmDeclarationOrigin = JvmDeclarationOrigin(
        originKind, descriptor.findPsi(), descriptor, parametersForJvmOverload
    )

    private fun DeclarationDescriptor.findPsi(): PsiElement? {
        val psi = (this as? DeclarationDescriptorWithSource)?.source?.getPsi()
        return if (psi == null && this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            overriddenDescriptors.mapNotNull { it.findPsi() }.firstOrNull()
        } else {
            psi
        }
    }

    private fun SourceElement.getPsi(): PsiElement? = (this as? PsiSourceElement)?.psi
}