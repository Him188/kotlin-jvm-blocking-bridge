@file:Suppress("RemoveRedundantBackticks", "RedundantSuspendModifier", "MainFunctionReturnUnit")

package compiler

import assertHasFunction
import assertNoFunction
import createInstance
import org.junit.jupiter.api.Test
import runFunction
import kotlin.coroutines.Continuation

internal class InheritanceTest : AbstractCompilerTest() {
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
        classLoader.loadClass("Abstract").run {
            assertHasFunction<String>("test")
        }
        classLoader.loadClass("TestData").assertNoFunction<String>("test", declaredOnly = true)
    }

    @Test
    fun `bridge for open fun`() = testJvmCompile(
        """
    sealed class S1 {
        @JvmBlockingBridge
        open suspend fun test(): String {
            return "OK"
        }
    }
    sealed class S2 : S1()
    object TestData : S2() {
        fun main(): String = TestData.runFunction("test")
    }
"""
    ) {
        classLoader.loadClass("S1").run {
            assertHasFunction<String>("test")
        }
        classLoader.loadClass("S2").run {
            assertNoFunction<String>("test", declaredOnly = true)
        }
        classLoader.loadClass("TestData").run {
            assertNoFunction<String>("test", declaredOnly = true)
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
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test")
        }
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
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test")
            createInstance().run {
                runFunction<String>("test")
            }
        }
    }

    @Test
    fun `bridge for interface inheritance`() = testJvmCompile(
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
        classLoader.loadClass("Interface2").run {
            assertHasFunction<String>("test")
            assertHasFunction<Any>("test", Continuation::class.java)
        }
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test")
            assertHasFunction<Any>("test", Continuation::class.java)
        }
        classLoader.loadClass("TestData").assertNoFunction<String>("test", declaredOnly = true)
    }

    @Test
    fun `interface override`() = testJvmCompile(
        """
    interface Interface2 {
        @JvmBlockingBridge
        suspend fun test(): String
    }
    interface TestData : Interface2 {
        @JvmBlockingBridge
        override suspend fun test() = "OK"
    }
""", noMain = true
    ) {
        classLoader.loadClass("Interface2").run {
            assertHasFunction<String>("test", declaredOnly = true)
            assertHasFunction<Any>("test", Continuation::class.java, declaredOnly = true)
        }
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test", declaredOnly = true)
            assertHasFunction<Any>("test", Continuation::class.java, declaredOnly = true)
        }
    }

    @Test
    fun `gen bridge for overridden`() = testJvmCompile(
        """
    interface Interface2 {
        @JvmBlockingBridge
        suspend fun test(): String
    }
    object TestData : Interface2 {
        @JvmBlockingBridge
        override suspend fun test() = "OK"
        
        fun main(): String = TestData.runFunction("test")
    }
"""
    ) {
        classLoader.loadClass("Interface2").run {
            assertHasFunction<String>("test")
            assertHasFunction<Any>("test", Continuation::class.java)
        }
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test")
            assertHasFunction<Any>("test", Continuation::class.java)
        }
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test", declaredOnly = true)
            assertHasFunction<Any>("test", Continuation::class.java, declaredOnly = true)
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
        classLoader.loadClass("Interface").run {
            assertHasFunction<String>("test")
        }
    }
}