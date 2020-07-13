package jvm

import org.junit.jupiter.api.Test

internal class TestCompilerInJvmBackend {

    @Test
    fun `simple function in object`() = testJvmCompile(
        """
        object TestData {
            @JvmBlockingBridge
            suspend fun test(): String{
                return "OK"
            }
            
            fun main(): String = this.runFunction("test")
        }
    """
    )
}
