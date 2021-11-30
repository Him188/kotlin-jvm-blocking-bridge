@file:Suppress("RedundantSuspendModifier")

package compiler

import assertHasFunction
import org.junit.jupiter.api.Test
import runFunction
import runStaticFunction
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

internal sealed class StaticTest(ir: Boolean) : AbstractCompilerTest(ir) {
    class Ir : StaticTest(true) {
        @Test
        fun test() {
            `static default with @JvmOverloads in companion`()
        }
    }

    class Jvm : StaticTest(false) {
        @Test
        fun test() {
            `static default in companion`()
            `static default with @JvmOverloads in companion`()
        }
    }


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
    fun `static default in companion`() = testJvmCompile(
        """
    class TestData {
        companion object {
            @JvmStatic
            @JvmBlockingBridge
            suspend fun String.test(d: Int = 1): String{
                assertEquals("receiver", this)
                assertEquals(1, d)
                return "OK"
            }
        }
    }
""", noMain = true
    ) {
        classLoader.loadClass("TestData").kotlin.companionObjectInstance?.run {
            this::class.java.run {
                assertHasFunction<String>("test", String::class, Int::class) // origin
            }
            runFunction<String>("test", "receiver", 1)
        }
        classLoader.loadClass("TestData")?.run {
            assertHasFunction<String>("test", String::class, Int::class) // origin

            runStaticFunction<String>("test", "receiver", 1)
        }
    }

    @Test
    fun `static default with @JvmOverloads in companion`() = testJvmCompile(
        """
    class TestData {
        companion object {
            @JvmStatic
            @JvmBlockingBridge
            @JvmOverloads
            suspend fun String.test(d: Int = 1): String{
                assertEquals("receiver", this)
                assertEquals(1, d)
                return "OK"
            }
        }
    }
""", noMain = true
    ) {
        classLoader.loadClass("TestData").kotlin.run {
            fun Class<*>.checkBoth() {
                assertHasFunction<String>("test", String::class, Int::class) // origin
                assertHasFunction<String>("test", String::class) // by @JvmDefault
            }

            java.run {
                checkBoth()

                runStaticFunction<String>("test", "receiver", 1)
                runStaticFunction<String>("test", "receiver")
            }
            companionObject!!.java.checkBoth()
            companionObjectInstance!!.run {
                runFunction<String>("test", "receiver", 1)
                runFunction<String>("test", "receiver")
            }
        }
    }

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
                getAnnotation(Class.forName("me.him188.kotlin.jvm.blocking.bridge.GeneratedBlockingBridge") as Class<Annotation>)
            return "OK"
        }
    }
"""
    )

}