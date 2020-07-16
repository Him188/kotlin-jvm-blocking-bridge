package jvm

import org.junit.jupiter.api.Test

internal class TestCompilerInJvmBackend {

    @Test
    fun `simple function in object`() = testJvmCompile(
        """
        object TestData {
            @JvmBlockingBridge
            suspend fun test() {}
            
            fun main(): String {
                this.runFunction<Unit>("test")
                return "OK"
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
}
