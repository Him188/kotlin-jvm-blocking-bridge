@file:Suppress("RedundantSuspendModifier")

package compiler

import assertHasFunction
import createInstance
import net.mamoe.kjbb.compiler.JvmBlockingBridgeCompilerConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.Test
import runFunction
import kotlin.test.assertEquals


internal sealed class BridgeForModule(ir: Boolean) : AbstractCompilerTest(ir) {
    override val overrideCompilerConfiguration: CompilerConfiguration = CompilerConfiguration().apply {
        put(JvmBlockingBridgeCompilerConfigurationKeys.ENABLE_FOR_MODULE, true.toString())
    }

    class Ir : BridgeForModule(true) {

        @Test
        fun test() {
            simple()
        }
    }

    class Jvm : BridgeForModule(false) {

        @Test
        fun test() {
            simple()
        }
    }

    @Test
    fun simple() = testJvmCompile("""
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
}