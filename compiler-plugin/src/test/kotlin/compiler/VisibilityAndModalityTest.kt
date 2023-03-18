package compiler

import org.jetbrains.kotlin.descriptors.Modality.FINAL
import org.jetbrains.kotlin.descriptors.Modality.OPEN
import org.jetbrains.kotlin.descriptors.Visibilities
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import kotlin.test.assertEquals

internal class VisibilityAndModalityTest : AbstractCompilerTest() {
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
            assertEquals(Modifier.FINAL or Modifier.PUBLIC, this.modifiers)
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
            assertEquals(Modifier.PUBLIC, this.modifiers)
        }
        classLoader.loadClass("TestData").getMethod("test2").run {
            assertEquals(Visibilities.Public, this.visibility)
            assertEquals(OPEN, this.modality)
            assertEquals(Modifier.PUBLIC, this.modifiers)
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
            assertEquals(Modifier.PUBLIC, this.modifiers)
        }
        classLoader.loadClass("TestData").getMethod("test2").run {
            assertEquals(Visibilities.Public, this.visibility)
            assertEquals(OPEN, this.modality)
            assertEquals(Modifier.PUBLIC, this.modifiers)
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
            assertEquals(Modifier.PUBLIC, this.modifiers)
        }
        classLoader.loadClass("TestData").getMethod("test2").run {
            assertEquals(Visibilities.Public, this.visibility)
            assertEquals(OPEN, this.modality)
            assertEquals(Modifier.PUBLIC, this.modifiers)
        }
    }
}