@file:Suppress("UNCHECKED_CAST", "RemoveRedundantBackticks")

package ir


import org.junit.jupiter.api.Test

@Suppress("RedundantSuspendModifier")
class TestCompilerInIrBackend {

    @Test
    fun `no param, no extension receiver`() = testIrCompile(
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

    @Test
    fun `has param, no extension receiver`() = testIrCompile(
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

    @Test
    fun `has param, has receiver`() = testIrCompile(
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

    @Test
    fun `jvm name`() = testIrCompile(
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
    fun `static`() = testIrCompile(
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