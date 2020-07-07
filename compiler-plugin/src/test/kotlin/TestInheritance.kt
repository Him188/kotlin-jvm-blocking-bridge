import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

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
    }

    /*
    @Test
    fun `class inheritance`() {
        testTestData(
            """
            abstract class Inter {
                @JvmBlockingBridge
                abstract suspend fun test(): String
            }
            object TestData: Inter() {
                override suspend fun test(): String{
                    return "OK"
                }
                fun main(): String = this.runFunction("test")
            }
        """
        )
    }*/
}