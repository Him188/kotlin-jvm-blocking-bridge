@file:Suppress("RemoveRedundantBackticks")

package compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


internal sealed class ExceptionTest(
    ir: Boolean,
) : AbstractCompilerTest(ir) {
    internal class Ir : ExceptionTest(ir = true)
    internal class Jvm : ExceptionTest(ir = false)

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

}