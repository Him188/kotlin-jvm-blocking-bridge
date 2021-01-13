@file:Suppress("RemoveRedundantBackticks", "RedundantSuspendModifier")

package compiler

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
            getMethod("test", String::class.java)
            getMethod("test")
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