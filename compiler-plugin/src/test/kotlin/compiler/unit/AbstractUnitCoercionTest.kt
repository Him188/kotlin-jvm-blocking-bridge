package compiler.unit

import compiler.AbstractCompilerTest
import org.junit.jupiter.api.Test

internal abstract class AbstractUnitCoercionTest : AbstractCompilerTest() {
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
                Class.forName("TestData").assertHasFunction<Void>("test", String::class.java, String::class.java)
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
                 Class.forName("TestData").assertHasFunction<String>("test", String::class.java)
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
        }
        
        fun main(): String {
            Class.forName("TestData").assertHasFunction<Void>("test", String::class.java, String::class.java)
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
            }
            
            @JvmStatic
            fun main(): String {
                Class.forName("TestData").assertHasFunction<Void>("test", String::class.java, String::class.java)
                Class.forName("TestData\${'$'}Companion").assertHasFunction<Void>("test", String::class.java, String::class.java)
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
                Class.forName("TestData").assertHasFunction<String>("test", String::class.java, String::class.java)
                Class.forName("TestData\${'$'}Companion").assertHasFunction<String>("test", String::class.java, String::class.java)
                
                return Class.forName("TestData").runStaticFunction("test", "receiver", "p0")
            }
        }
    }
"""
    )
}