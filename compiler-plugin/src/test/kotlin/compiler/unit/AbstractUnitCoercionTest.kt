package compiler.unit

import compiler.AbstractCompilerTest
import org.junit.jupiter.api.Test

internal abstract class AbstractUnitCoercionTest(ir: Boolean) : AbstractCompilerTest(ir) {
    @Test
    fun member() = testJvmCompile(
        """
    class TestData {
        @JvmBlockingBridge
        suspend fun String.test(arg: String) { // returns Unit
            assertEquals("receiver", this)
            assertEquals("p0", arg)
        }
        
        companion object {
            @JvmStatic
            fun main(): String {
                assertEquals("void", Class.forName("TestData").getFunctionReturnType("test", "receiver", "p0"))
                return "OK"
            }
        }
    }
"""
    )

    @Test
    fun `member non unit return type`() = testJvmCompile(
        """
    class TestData {
        @JvmBlockingBridge
        suspend fun test(arg: String): String { // returns Unit
            return ""
        }
        
        companion object {
            @JvmStatic
            fun main(): String {
                assertEquals("java.lang.String", Class.forName("TestData").getFunctionReturnType("test", "p0"))
                return "OK"
            }
        }
    }
"""
    )

    @Test
    fun static() = testJvmCompile(
        """
    object TestData {
        @JvmStatic
        @JvmBlockingBridge
        suspend fun String.test(arg: String) { // returns Unit
            assertEquals("receiver", this)
            assertEquals("p0", arg)
        }
        
        fun main(): String {
            assertEquals("void", Class.forName("TestData").getFunctionReturnType("test", "receiver", "p0"))
            return "OK"
        }
    }
"""
    )

    @Test
    fun `static companion`() = testJvmCompile(
        """
    class TestData {
        companion object {
            @JvmStatic
            @JvmBlockingBridge
            suspend fun String.test(arg: String) { // returns Unit
                assertEquals("receiver", this)
                assertEquals("p0", arg)
            }
            
            @JvmStatic
            fun main(): String {
                assertEquals("void", Class.forName("TestData\${"$"}Companion").getFunctionReturnType("test", "receiver", "p0"))
                assertEquals("void", Class.forName("TestData").getFunctionReturnType("test", "receiver", "p0"))
                return "OK"
            }
        }
    }
"""
    )


    @Test
    fun `static companion non unit`() = testJvmCompile(
        """
    class TestData {
        companion object {
            @JvmStatic
            @JvmBlockingBridge
            suspend fun String.test(arg: String): String { // returns Unit
                assertEquals("receiver", this)
                assertEquals("p0", arg)
                return "OK"
            }
            
            @JvmStatic
            fun main(): String {
                assertEquals("java.lang.String", Class.forName("TestData\${"$"}Companion").getFunctionReturnType("test", "receiver", "p0"))
                assertEquals("java.lang.String", Class.forName("TestData").getFunctionReturnType("test", "receiver", "p0"))
                
                return Class.forName("TestData").runStaticFunction("test", "receiver", "p0")
            }
        }
    }
"""
    )
}