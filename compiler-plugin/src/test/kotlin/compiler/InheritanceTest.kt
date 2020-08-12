@file:Suppress("RemoveRedundantBackticks", "RedundantSuspendModifier", "MainFunctionReturnUnit")

package compiler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

internal sealed class InheritanceTest(
    ir: Boolean,
) : AbstractCompilerTest(ir) {

    internal class Ir : InheritanceTest(true)
    internal class Jvm : InheritanceTest(false)

    @Test
    fun `bridge for abstract`() = testJvmCompile(
        """
    abstract class Abstract {
        @JvmBlockingBridge
        abstract suspend fun test(): String
    }
    object TestData : Abstract() {
        override suspend fun test() = "OK"
        
        fun main(): String = TestData.runFunction("test")
    }
"""
    ) {
        assertFailsWith<NoSuchMethodException> {
            assertThat(classLoader.loadClass("Abstract")).hasDeclaredMethods("test")
            classLoader.loadClass("TestData").getDeclaredMethod("test")
        }
    }

    @Test
    fun `bridge for overridden`() = testJvmCompile(
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
    ) {
        classLoader.loadClass("TestData").getDeclaredMethod("test")
    }

    @Test
    fun `bridge for interface overriding`() = testJvmCompile(
        """
    interface Interface3 {
        suspend fun test(): String
    }
    object TestData : Interface3 {
        @JvmBlockingBridge
        override suspend fun test() = "OK"
        
        fun main(): String = TestData.runFunction("test")
    }
"""
    ) {
        classLoader.loadClass("TestData").getDeclaredMethod("test")
    }

    @Test
    open fun `bridge for interface inheritance`() = testJvmCompile(
        """
    interface Interface2 {
        @JvmBlockingBridge
        suspend fun test(): String
    }
    object TestData : Interface2 {
        override suspend fun test() = "OK"
        
        fun main(): String = TestData.runFunction("test")
    }
"""
    ) {
        assertThat(classLoader.loadClass("Interface2")).hasDeclaredMethods("test")
        assertFailsWith<NoSuchMethodException> {
            classLoader.loadClass("TestData").getDeclaredMethod("test")
        }
    }

    @Test
    fun `interface codegen`() = testJvmCompile(
        """
    interface Interface {
        @JvmBlockingBridge
        suspend fun test(): String
    }
""", noMain = true
    ) {
        assertThat(classLoader.loadClass("Interface")).hasDeclaredMethods("test")
    }
}