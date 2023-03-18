@file:Suppress("RemoveRedundantBackticks", "RedundantSuspendModifier")

package compiler

import assertHasFunction
import assertNoFunction
import org.junit.jupiter.api.Test
import runFunction
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


internal class AbiAnnotationsTest : AbstractCompilerTest() {

    @Test
    fun `exceptions`() = testJvmCompile(
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
    fun `jvm overloads`() = testJvmCompile(
        """
            class TestData {
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

            this.getConstructor().newInstance().runFunction<Any?>("test", "")
            this.getConstructor().newInstance().runFunction<Any?>("test")
        }
    }
    
    @Test
    fun `no jvm overloads`() = testJvmCompile(
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