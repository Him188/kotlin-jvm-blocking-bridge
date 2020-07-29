package ir

import org.junit.jupiter.api.Test
import kotlin.test.assertFails

internal class TestForInterface {

    @Test
    fun `interface inheritance`() {
        assertFails {
            testIrCompile(
                """
            interface Inter {
                @JvmBlockingBridge // inapplicable
                suspend fun test(): String
            }
        """
            )
        }
    }

/*
    @Test
    fun `class inheritance`() {
        testIrCompile(
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
    }*/
}