package compiler

import modality
import org.jetbrains.kotlin.descriptors.Modality.FINAL
import org.jetbrains.kotlin.descriptors.Modality.OPEN
import org.jetbrains.kotlin.descriptors.Visibilities.PUBLIC
import org.junit.jupiter.api.Test
import visibility
import kotlin.test.assertEquals

internal sealed class VisibilityAndModalityTest(
    ir: Boolean,
) : AbstractCompilerTest(ir) {
    internal class Ir : VisibilityAndModalityTest(ir = true)
    internal class Jvm : VisibilityAndModalityTest(ir = false)

    @Test
    fun `public final bridge for public final function in public final object`() = testJvmCompile(
        """
            object TestData {
                @JvmBlockingBridge
                suspend fun test() {}
            }
        """, noMain = true
    ) {
        classLoader.loadClass("TestData").getMethod("test").run {
            assertEquals(PUBLIC, this.visibility)
            assertEquals(FINAL, this.modality)
        }
    }

    @Test
    fun `public open bridge for interfaces`() = testJvmCompile(
        """
            interface TestData {
                @JvmBlockingBridge
                suspend fun test() {}
                
                @JvmBlockingBridge
                suspend fun test2()
            }
        """, noMain = true
    ) {
        classLoader.loadClass("TestData").getMethod("test").run {
            assertEquals(PUBLIC, this.visibility)
            assertEquals(OPEN, this.modality)
        }
        classLoader.loadClass("TestData").getMethod("test2").run {
            assertEquals(PUBLIC, this.visibility)
            assertEquals(OPEN, this.modality)
        }
    }

    @Test
    fun `public final bridge for abstract classes`() = testJvmCompile(
        """
            abstract class TestData {
                @JvmBlockingBridge
                open suspend fun test() {}
                
                @JvmBlockingBridge
                abstract suspend fun test2()
            }
        """, noMain = true
    ) {
        classLoader.loadClass("TestData").getMethod("test").run {
            assertEquals(PUBLIC, this.visibility)
            assertEquals(OPEN, this.modality)
        }
        classLoader.loadClass("TestData").getMethod("test2").run {
            assertEquals(PUBLIC, this.visibility)
            assertEquals(OPEN, this.modality)
        }
    }
}