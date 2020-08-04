package net.mamoe.kjbb.compiler.context

import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import java.util.*

/**
 * For resolving `org.jetbrains.kotlin.com.intellij.psi.PsiElement` for CLI compiler
 * and `com.intellij.psi.PsiElement` for IntelliJ built-in compiler (Kotlin plugin for IntelliJ)
 */
@Suppress("FunctionName")
interface CompilerContext {
    val kind: CompilerContextKind

    fun FunctionDescriptor.isGeneratedStubForJavaResolving(): Boolean
    fun KtLightMethod.setGeneratedStubForJavaResolving()

    fun JvmDeclarationOrigin(
        originKind: JvmDeclarationOriginKind,
        descriptor: DeclarationDescriptor,
        parametersForJvmOverload: List<KtParameter?>? = listOf()
    ): JvmDeclarationOrigin

    sealed class CompilerContextKind {
        object IntelliJ : CompilerContextKind()
        object CLI : CompilerContextKind() // including gradle
    }

    companion object {
        val INSTANCE by lazy {
            val list = ServiceLoader.load(CompilerContext::class.java)

            val intellij = list.firstOrNull { it.kind is CompilerContextKind.IntelliJ }

            if (intellij != null) {
                return@lazy intellij
            } else {
                return@lazy list.firstOrNull() ?: CompilerContextCli
            }
        }
    }
}