@file:Suppress("UNCHECKED_CAST", "RemoveRedundantBackticks")


import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import net.mamoe.kjbb.JvmBlockingBridgeComponentRegistrar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.reflect.Modifier
import kotlin.test.assertEquals

// Expose to top-package for TestData
typealias JvmBlockingBridge = net.mamoe.kjbb.JvmBlockingBridge

fun <R> Any.runFunction(name: String, vararg args: Any): R {
    return this::class.java.getMethod(name, *args.map { it::class.java }.toTypedArray()).invoke(this, *args) as R
}

fun <R> Class<*>.runStaticFunction(name: String, vararg args: Any): R {
    return getMethod(name, *args.map { it::class.java }.toTypedArray()).also {
        assert(Modifier.isStatic(it.modifiers)) { "method $name is not static" }
    }.invoke(null, *args)!! as R
}

internal fun test(
    @Language("kt")
    source: String
) {
    val kotlinSource = SourceFile.kotlin(
        "TestData.kt", "import kotlin.test.assertEquals\n$source"
    )

    val result = KotlinCompilation().apply {
        sources = listOf(kotlinSource)

        compilerPlugins = listOf(JvmBlockingBridgeComponentRegistrar())
        verbose = false

        workingDir = File("testCompileOutput").apply { mkdir() }

        useIR = true

        inheritClassPath = true
        messageOutputStream = System.out
    }.compile()

    assert(result.exitCode == KotlinCompilation.ExitCode.OK)

    @Suppress("UNCHECKED_CAST")
    val test = result.classLoader.loadClass("TestData")
    assertEquals(
        "OK",
        (test.kotlin.objectInstance!!).run {
            this::class.java.methods.first { it.name == "main" }.invoke(this)
        } as String)
}

@Suppress("RedundantSuspendModifier")
class TestCompiling {

    @Test
    fun `no arg, no extension receiver`() {
        test(
            """
        object TestData {
            @JvmBlockingBridge
            suspend fun test(): String{
                return "OK"
            }
            
            fun main(): String = this.runFunction("test")
        }
    """
        )
    }

    @Test
    fun `has arg, no extension receiver`() {
        test(
            """
        object TestData {
            @JvmBlockingBridge
            suspend fun test(arg: String): String{
                assertEquals("p0", arg)
                return "OK"
            }
            
            fun main(): String = this.runFunction("test", "p0")
        }
    """
        )
    }

    @Test
    fun `has arg, has receiver`() {
        test(
            """
        import kotlin.test.assertEquals
        object TestData {
            @JvmStatic
            @JvmBlockingBridge
            suspend fun String.test(arg: String): String{
                assertEquals("receiver", this)
                assertEquals("p0", arg)
                return "OK"
            }
            
            fun main(): String = this.runFunction("test", "receiver", "p0")
        }
    """
        )
    }

    @Test
    fun `static`() {
        test(
            """
        import kotlin.test.assertEquals
        object TestData {
            @JvmStatic
            @JvmBlockingBridge
            suspend fun String.test(arg: String): String{
                assertEquals("receiver", this)
                assertEquals("p0", arg)
                return "OK"
            }
            
            fun main(): String = Class.forName("TestData").runStaticFunction("test", "receiver", "p0")
        }
    """
        )
    }
}