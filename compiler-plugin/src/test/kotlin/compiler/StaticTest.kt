@file:Suppress("RedundantSuspendModifier")

package compiler

import org.junit.jupiter.api.Test

internal sealed class StaticTest(ir: Boolean) : AbstractCompilerTest(ir) {
    class Ir : StaticTest(true)
    class Jvm : StaticTest(false)


    @Test
    fun static() = testJvmCompile(
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

    @Test
    fun `static function in class`() = testJvmCompile(
        """
    class TestData {
        companion object {
            @JvmStatic
            @JvmBlockingBridge
            suspend fun String.test(arg: String): String{
                assertEquals("receiver", this)
                assertEquals("p0", arg)
                return "OK"
            }
        }
        
        fun main(): String = Class.forName("TestData").runStaticFunction("test", "receiver", "p0")
    }
"""
    )

    @Test
    fun `member function in class companion`() = testJvmCompile(
        """
    class TestData {
        companion object {
            @JvmStatic
            @JvmBlockingBridge
            suspend fun String.test(arg: String): String{
                assertEquals("receiver", this)
                assertEquals("p0", arg)
                return "OK"
            }
        
            fun main(): String = this.runFunction("test", "receiver", "p0")
        }
    }
"""
    )

    @Test
    fun `static function in interface`() = testJvmCompile(
        """
    interface TestData {
        companion object {
            @JvmStatic
            @JvmBlockingBridge
            suspend fun String.test(arg: String): String{
                assertEquals("receiver", this)
                assertEquals("p0", arg)
                return "OK"
            }
        
            fun main(): String = Class.forName("TestData").runStaticFunction("test", "receiver", "p0")
        }
    }
"""
    )

    @Test
    fun `member function in interface companion`() = testJvmCompile(
        """
    interface TestData {
        companion object {
            @JvmStatic
            @JvmBlockingBridge
            suspend fun String.test(arg: String): String{
                assertEquals("receiver", this)
                assertEquals("p0", arg)
                return "OK"
            }
        
            fun main(): String = this.runFunction("test", "receiver", "p0")
        }
    }
"""
    )

    @Test
    fun `GeneratedJvmBlockingBridge annotation on static bridge`() = testJvmCompile(
        """
    class TestData {
        companion object {
            @JvmStatic
            @JvmBlockingBridge
            suspend fun test(arg: String) {}
        }
        
        fun main(): String {
            Class.forName("TestData").getMethod("test", String::class.java). 
                getAnnotation(Class.forName("net.mamoe.kjbb.GeneratedBlockingBridge") as Class<Annotation>)
            return "OK"
        }
    }
"""
    )

}