package compiler

import modality
import org.jetbrains.kotlin.descriptors.Modality.FINAL
import org.jetbrains.kotlin.descriptors.Modality.OPEN
import org.jetbrains.kotlin.descriptors.Visibilities
import org.junit.jupiter.api.Test
import visibility
import kotlin.test.assertEquals

internal sealed class VisibilityAndModalityTest(
    ir: Boolean,
) : AbstractCompilerTest(ir) {
    internal class Ir : VisibilityAndModalityTest(ir = true)
    internal class Jvm : VisibilityAndModalityTest(ir = false)

    @Test
    fun `final bridge for final function in final object`() = testJvmCompile(
        """
            object TestData {
                @JvmBlockingBridge
                suspend fun test() {}
            }
        """, noMain = true
    ) {
        classLoader.loadClass("TestData").getMethod("test").run {
            assertEquals(Visibilities.Public, this.visibility)
            assertEquals(FINAL, this.modality)
        }
    }

    @Test
    fun `open bridge for interfaces`() = testJvmCompile(
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
            assertEquals(Visibilities.Public, this.visibility)
            assertEquals(OPEN, this.modality)
        }
        classLoader.loadClass("TestData").getMethod("test2").run {
            assertEquals(Visibilities.Public, this.visibility)
            assertEquals(OPEN, this.modality)
        }
    }

    @Test
    fun `open bridge for abstract classes`() = testJvmCompile(
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
            assertEquals(Visibilities.Public, this.visibility)
            assertEquals(OPEN, this.modality)
        }
        classLoader.loadClass("TestData").getMethod("test2").run {
            assertEquals(Visibilities.Public, this.visibility)
            assertEquals(OPEN, this.modality)
        }
    }

    @Test
    fun `open bridge for sealed classes`() = testJvmCompile(
        """
            sealed class TestData {
                @JvmBlockingBridge
                open suspend fun test() {}
                
                @JvmBlockingBridge
                abstract suspend fun test2()
            }
        """, noMain = true
    ) {
        classLoader.loadClass("TestData").getMethod("test").run {
            assertEquals(Visibilities.Public, this.visibility)
            assertEquals(OPEN, this.modality)
        }
        classLoader.loadClass("TestData").getMethod("test2").run {
            assertEquals(Visibilities.Public, this.visibility)
            assertEquals(OPEN, this.modality)
        }
    }
}