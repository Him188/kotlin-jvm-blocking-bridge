package compiler

import com.tschuchort.compiletesting.KotlinCompilation
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import testJvmCompile

internal abstract class AbstractCompilerTest(
    private val ir: Boolean,
) {
    protected open val overrideCompilerConfiguration: CompilerConfiguration? = null

    @DslMarker
    annotation class CompilerTestClause

    @CompilerTestClause
    fun testJvmCompile(
        @Language("kt")
        kt: String,
        @Language("java")
        java: String? = null,
        noMain: Boolean = false,
        jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
        overrideCompilerConfiguration: CompilerConfiguration? = this.overrideCompilerConfiguration,
        config: KotlinCompilation.() -> Unit = {},
        block: KotlinCompilation.Result.() -> Unit = {},
    ) = testJvmCompile(kt, java, noMain, ir, jvmTarget, overrideCompilerConfiguration, config, block = block)
}