@file:Suppress("RemoveRedundantBackticks", "RedundantSuspendModifier")

package compiler

import assertHasFunction
import assertNoFunction
import net.mamoe.kjbb.compiler.JvmBlockingBridgeCompilerConfigurationKeys
import net.mamoe.kjbb.compiler.UnitCoercion
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


internal sealed class AbiAnnotationsTest(
    ir: Boolean,
) : AbstractCompilerTest(ir) {
    internal class Ir : AbiAnnotationsTest(ir = true)
    internal class Jvm : AbiAnnotationsTest(ir = false)

    @Test
    open fun `exceptions`() = testJvmCompile(
        """
            object TestData {
                @Throws(java.io.IOException::class)
                @JvmBlockingBridge
                suspend fun test() {}
            }
        """, noMain = true
    ) {
        classLoader.loadClass("TestData").getMethod("test").run {
            assertEquals("java.io.IOException", this.exceptionTypes.single().canonicalName)
        }
    }

    @Test
    open fun `jvm overloads`() = testJvmCompile(
        """
            object TestData {
                @JvmOverloads
                @JvmBlockingBridge
                suspend fun test(a: String = "") {}
            }
        """, noMain = true
    ) {
        classLoader.loadClass("TestData").run {
            assertHasFunction<Void>("test", String::class.java)
            assertNoFunction<Unit>("test", String::class.java)
            assertHasFunction<Void>("test")
        }
    }

    @Test
    open fun `jvm overloads with unit coercion compatibility`() = testJvmCompile(
        """
            object TestData {
                @JvmOverloads
                @JvmBlockingBridge
                suspend fun test(b: Boolean = true, a: String = "") {}
            }
        """, noMain = true, overrideCompilerConfiguration = CompilerConfiguration().apply {
            put(JvmBlockingBridgeCompilerConfigurationKeys.UNIT_COERCION, UnitCoercion.COMPATIBILITY.toString())
        }
    ) {
        classLoader.loadClass("TestData").run {
            assertHasFunction<Void>("test", Boolean::class.javaPrimitiveType!!, String::class.java)
            assertHasFunction<Void>("test", Boolean::class.javaPrimitiveType!!)
            assertHasFunction<Void>("test")
        }
    }

    @Test
    open fun `no jvm overloads`() = testJvmCompile(
        """
            object TestData {
                @JvmBlockingBridge
                suspend fun test(a: String = "") {}
            }
        """, noMain = true
    ) {
        classLoader.loadClass("TestData").run {
            getMethod("test", String::class.java)
            assertFailsWith<NoSuchMethodException> { getMethod("test") }
        }
    }
}