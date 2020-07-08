import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

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
        assertFailsWith<IllegalStateException> {
            testTestData(
                """
            interface Inter {
                @JvmBlockingBridge
                suspend fun test(): String
            }
            object TestData: Inter {
                override suspend fun test(): String {
                    return "OK"
                }
                fun main(): String = this.runFunction("test")
            }
        """
            )
        }.let { e ->
            assert(e.message?.startsWith("@JvmBlockingBridge is not applicable to function ") == true) { e.message.toString() }
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