@file:Suppress("RedundantSuspendModifier")

package compiler

import assertHasFunction
import assertNoFunction
import createInstance
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.Test
import runFunction
import kotlin.test.assertEquals


internal class BridgeForModule : AbstractCompilerTest() {
    override val overrideCompilerConfiguration: CompilerConfiguration = CompilerConfiguration().apply {
        put(
            me.him188.kotlin.jvm.blocking.bridge.compiler.JvmBlockingBridgeCompilerConfigurationKeys.ENABLE_FOR_MODULE,
            true.toString()
        )
    }

    @Test
    fun simple() = testJvmCompile(
        """
        object TestData {
            suspend fun test() = "OK"
        }
    """, noMain = true
    ) {
        classLoader.loadClass("TestData").apply {
            assertHasFunction<String>("test")
            assertEquals("OK", createInstance().runFunction("test"))
        }
    }

    @Test
    fun static() = testJvmCompile(
        """
    object TestData {
        @JvmStatic
        suspend fun test(arg: String) { // returns Unit
        }
        
        fun main(): String {
            Class.forName("TestData").assertHasFunction<Void>("test", String::class.java) 
            return "OK"
        }
    }
"""
    )

    @Test
    fun inheritance() = testJvmCompile(
        """
    interface A {
        suspend fun test(arg: String) {}
    }
    interface B : A {
        override suspend fun test(arg: String) {}
    }
""", noMain = true
    ) {
        classLoader.loadClass("A").assertHasFunction<Void>("test", String::class, declaredOnly = true)
        classLoader.loadClass("B").assertHasFunction<Void>("test", String::class, declaredOnly = true)
    }

    @Test
    fun `static companion`() = testJvmCompile(
        """
    class TestData {
        companion object {
            @JvmStatic
            suspend fun test(arg: String) { // returns Unit
            }
            
            @JvmStatic
            fun main(): String {
                Class.forName("TestData").assertHasFunction<Void>("test", String::class.java) 
                return "OK"
            }
        }
    }
"""
    )

    @Test
    fun `synthetic in static companion`() = testJvmCompile(
        """
    class TestData {
        companion object {
            @JvmSynthetic
            @JvmStatic
            suspend fun test() {
            }
        }
    }
""", noMain = true
    ) {
        classLoader.loadClass("TestData").run {
            assertNoFunction<Void>("test")
            assertNoFunction<Unit>("test")
        }
        classLoader.loadClass("TestData\$Companion").run {
            assertNoFunction<Void>("test")
            assertNoFunction<Unit>("test")
        }
    }
}