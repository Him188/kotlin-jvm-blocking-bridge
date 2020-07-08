import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import kotlin.test.assertFails

private fun test(
    @Language("kt")
    source: String
) {
    val result = compile(source)

    val testData = result.classLoader.loadClass("TestData")
}

internal class TestForInterface {

    @Test
    fun `interface inheritance`() {
        assertFails {
            testTestData(
                """
            interface Inter {
                @JvmBlockingBridge // inapplicable
                suspend fun test(): String
            }
        """
            )
        }
    }


    @Test
    fun `class inheritance`() {
        testTestData(
            """
            abstract class Abs {
                @JvmBlockingBridge
                open suspend fun test(): String = "NOT OK"
            }
            object TestData : Abs() {
                override suspend fun test(): String{
                    return "OK"
                }
                fun main(): String = this.runFunction("test")
            }
        """
        )
    }
}