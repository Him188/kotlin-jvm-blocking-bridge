package jvm

import org.junit.jupiter.api.Test

internal class TestCompilerInJvmBackend {

    @Test
    fun `function with param in object`() = testJvmCompile(
        """
        object TestData {
            @JvmBlockingBridge
            suspend fun test(arg1: String): String{
                return arg1
            }
            
            fun main(): String = this.runFunction("test", "OK")
        }
    """
    )

    @Test
    fun `simple function in object`() = testJvmCompile(
        """
        object TestData {
            @JvmBlockingBridge
            suspend fun test() {}
            
            fun main(): String {
                this.runFunction<Unit>("test")
                return "OK"
            }
        }
    """
    )
}
