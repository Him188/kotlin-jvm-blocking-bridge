package compiler

import com.tschuchort.compiletesting.KotlinCompilation
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.JvmTarget
import testJvmCompile

internal abstract class AbstractCompilerTest(
    private val ir: Boolean,
) {
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
        block: KotlinCompilation.Result.() -> Unit = {},
    ) = testJvmCompile(kt, java, noMain, ir, jvmTarget, block)
}