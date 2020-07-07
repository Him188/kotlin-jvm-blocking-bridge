@file:Suppress("UNCHECKED_CAST", "RemoveRedundantBackticks")


import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

fun testTestData(
    @Language("kt")
    source: String
) {
    val result = compile(source)

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
    fun `no param, no extension receiver`() {
        testTestData(
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
    fun `has param, no extension receiver`() {
        testTestData(
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
    fun `has param, has receiver`() {
        testTestData(
            """
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
        testTestData(
            """
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