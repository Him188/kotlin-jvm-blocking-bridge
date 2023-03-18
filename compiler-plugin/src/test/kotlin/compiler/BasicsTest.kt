@file:Suppress("RemoveRedundantBackticks", "RedundantSuspendModifier", "MainFunctionReturnUnit")

package compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class BasicsTest : AbstractCompilerTest() {
    @Test
    fun `topLevel`() = testJvmCompile(
        """
    @JvmBlockingBridge
    suspend fun test() = "OK"
""", noMain = true
    ) {
        assertEquals("OK", classLoader.loadClass("TestData0Kt").getDeclaredMethod("test").invoke(null))
    }

    @Test
    fun `simple function in object`() = testJvmCompile(
        """
import java.lang.reflect.Modifier
object TestData {
            @JvmBlockingBridge
            suspend fun test() {}
            
            fun main(): String {
                this.runFunction<Unit>("test")
                return "OK"
            }
        }
    """,
        """
            public class J {
                public void j() {
                    TestData.INSTANCE.test();
                }
            }
        """
    )

    @Test
    fun `function with many param in object`() = testJvmCompile(
        """
        object TestData {
            @JvmBlockingBridge
            suspend fun test(arg1: String, arg2: String, arg3: String): String{
                assertEquals("KO", arg2)
                assertEquals("OO", arg3)
                return arg1
            }
            
            fun main(): String = this.runFunction("test", "OK", "KO", "OO")
        }
    """
    )

    @Test
    fun `function with receiver in object`() = testJvmCompile(
        """
        object TestData {
            @JvmBlockingBridge
            suspend fun String.test(arg1: String): String{
                assertEquals("aaa", this)
                return arg1
            }
            
            fun main(): String = this.runFunction("test", "aaa", "OK")
        }
    """
    )

    @Test
    fun `function with primitives2 in object`() = testJvmCompile(
        """
        object TestData {
            @JvmBlockingBridge
            suspend fun test(int: Int, float: Float, double: Double, char: Char, boolean: Boolean, short: Short): String{
                assertEquals(123, int)
                assertEquals(123f, float)
                assertEquals(123.0, double)
                assertEquals('1', char)
                assertEquals(true, boolean)
                assertEquals(123, short)
                return "OK"
            }
            
            fun main(): String = this.runFunction("test", 123,  123f,  123.0, '1', true, 123.toShort())
        }
    """
    )

    @Test
    fun `function in class`() = testJvmCompile(
        """
            abstract class SuperClass {
                @JvmBlockingBridge
                suspend fun test(value: String): String {
                    assertEquals("v", value)
                    return "OK"
                }
            }
            
            object TestData : SuperClass() {
                fun main(): String = this.runFunction("test", "v")
            }
    """
    )

    @Test
    fun `jvm name`() = testJvmCompile(
        """
    object TestData {
        @kotlin.jvm.JvmName("test")
        @JvmBlockingBridge
        suspend fun String.test2(arg: String): String{
            assertEquals("receiver", this)
            assertEquals("p0", arg)
            return "OK"
        }
        
        fun main(): String = this.runFunction("test", "receiver", "p0")
    }
"""
    )

    @Test
    fun `abstract`() = testJvmCompile(
        """
            
    abstract class Abstract {
        abstract suspend fun test(): String
    }
    object TestData : Abstract() {
        @JvmBlockingBridge
        override suspend fun test() = "OK"
        
        fun main(): String = TestData.runFunction("test")
    }
"""
    )

    @Test
    fun `mangling`() = testJvmCompile(
        """
    object TestData {
        @JvmBlockingBridge
        suspend fun test() = "OK"
        @JvmBlockingBridge
        suspend fun test(s: String) = "OK"
        
        fun main(): String = TestData.runFunction("test")
    }
"""
    )
}
