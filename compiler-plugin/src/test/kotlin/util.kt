@file:Suppress("UNCHECKED_CAST", "unused")

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import net.mamoe.kjbb.JvmBlockingBridge
import org.intellij.lang.annotations.Language
import java.io.File
import java.lang.reflect.Modifier
import java.util.*

// Expose to top-package for TestData
typealias JvmBlockingBridge = JvmBlockingBridge

fun <R> Any.runFunction(name: String, vararg args: Any): R {
    return this::class.java.getMethod(name, *args.map { it::class.javaPrimitiveType ?: it::class.java }.toTypedArray())
        .invoke(this, *args) as R
}

fun <R> Class<*>.runStaticFunction(name: String, vararg args: Any): R {
    return getMethod(name, *args.map { it::class.java }.toTypedArray()).also {
        assert(Modifier.isStatic(it.modifiers)) { "method $name is not static" }
    }.invoke(null, *args)!! as R
}

fun compile(
    @Language("kt")
    source: String,
    ir: Boolean
) = compile(source, null, ir)

fun compile(
    @Language("kt")
    source: String,
    @Language("java")
    java: String? = null,
    ir: Boolean
): KotlinCompilation.Result {
    val intrinsicImports = listOf(
        "import kotlin.test.*",
        "import JvmBlockingBridge"
    )

    val kotlinSource = if (source.trim().startsWith("package")) {
        SourceFile.kotlin("TestData.kt", run {
            source.trimIndent().lines().mapTo(LinkedList()) { it }
                .apply { addAll(1, intrinsicImports) }
                .joinToString("\n")
        })
    } else {
        SourceFile.kotlin(
            "TestData.kt", "${intrinsicImports.joinToString("\n")}\n${source.trimIndent()}"
        )
    }

    return KotlinCompilation().apply {
        sources = listOfNotNull(
            kotlinSource,
            java?.let { javaSource ->
                SourceFile.java(
                    Regex("""class\s*(.*?)\s*\{""").find(javaSource)!!.groupValues[1].let { "$it.java" },
                    javaSource
                )
            }
        )

        compilerPlugins = listOf(TestComponentRegistrar())
        verbose = false

        workingDir = File("testCompileOutput").apply {
            this.walk().forEach { it.delete() }
            mkdir()
        }

        useIR = ir

        inheritClassPath = true
        messageOutputStream = System.out
    }.compile().also { result ->
        assert(result.exitCode == KotlinCompilation.ExitCode.OK)
    }
}
