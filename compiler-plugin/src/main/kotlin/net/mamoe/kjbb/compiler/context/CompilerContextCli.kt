package net.mamoe.kjbb.compiler.context

import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

object CompilerContextCli : CompilerContext {
    override val kind: CompilerContext.CompilerContextKind
        get() = CompilerContext.CompilerContextKind.CLI

    override fun FunctionDescriptor.isGeneratedStubForJavaResolving(): Boolean = false
    override fun KtLightMethod.setGeneratedStubForJavaResolving() {
        // no-op
    }

    override fun JvmDeclarationOrigin(
        originKind: JvmDeclarationOriginKind,
        descriptor: DeclarationDescriptor,
        parametersForJvmOverload: List<KtParameter?>?
    ): JvmDeclarationOrigin {
        return JvmDeclarationOrigin(
            originKind, descriptor.findPsi(), descriptor, parametersForJvmOverload
        )
    }
}