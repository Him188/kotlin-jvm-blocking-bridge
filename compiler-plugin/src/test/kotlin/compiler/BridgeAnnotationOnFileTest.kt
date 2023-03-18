package compiler

import assertHasFunction
import createInstance
import org.junit.jupiter.api.Test
import runFunction
import kotlin.coroutines.Continuation
import kotlin.test.assertFailsWith

internal class BridgeAnnotationOnFileTest : AbstractCompilerTest() {

    @Test
    fun `suspend gen`() = testJvmCompile(
        """
            @file:JvmBlockingBridge
            object TestData {
                suspend fun test() {}
            }
        """, noMain = true
    ) {
        classLoader.loadClass("TestData").kotlin.objectInstance!!.runFunction<Void>("test")
    }

    @Test
    fun `non suspend should be ok`() = testJvmCompile(
        """
            @file:JvmBlockingBridge
            object TestData {
                fun test() {}
            }
        """, noMain = true
    )

    @Test
    fun `no inspection even inapplicable`() = testJvmCompile(
        """
            @file:JvmBlockingBridge
            object TestData {
                @JvmSynthetic
                suspend fun test() {}
            
                private suspend fun test2() {}
            }
        """, noMain = true
    ) {
        classLoader.loadClass("TestData")
    }


    ///////////////////////////////////////////////////////////////////////////
    // inheritance
    // copied from InheritanceTest and changed places of annotations
    ///////////////////////////////////////////////////////////////////////////

    @Test
    fun `bridge for abstract`() = testJvmCompile(
        """
        @file:JvmBlockingBridge
    abstract class Abstract {
        abstract suspend fun test(): String
    }

    $FILE_SPLITTER

    object TestData : Abstract() {
        override suspend fun test() = "OK"
        
        fun main(): String = TestData.runFunction("test")
    }
""".trimIndent()
    ) {
        assertFailsWith<NoSuchMethodException> {
            classLoader.loadClass("Abstract").run {
                assertHasFunction<String>("test")
            }
            classLoader.loadClass("TestData").getDeclaredMethod("test")
        }
    }

    @Test
    fun `bridge for overridden`() = testJvmCompile(
        """
    abstract class Abstract {
        abstract suspend fun test(): String
    }

    $FILE_SPLITTER

    @file:JvmBlockingBridge
    object TestData : Abstract() {
        override suspend fun test() = "OK"
        
        fun main(): String = TestData.runFunction("test")
    }
""".trimIndent()
    ) {
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test")
        }
    }

    @Test
    fun `bridge for interface overriding`() = testJvmCompile(
        """
    interface Interface3 {
        suspend fun test(): String
    }

    $FILE_SPLITTER
        @file:JvmBlockingBridge
    object TestData : Interface3 {
        override suspend fun test() = "OK"
    }
""".trimIndent(), noMain = true
    ) {
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test")
            createInstance().run {
                runFunction<String>("test")
            }
        }
    }

    @Test
    fun `bridge for interface inheritance`() = testJvmCompile(
        """
        @file:JvmBlockingBridge
    interface Interface2 {
        suspend fun test(): String
    }
  
    $FILE_SPLITTER
  object TestData : Interface2 {
        override suspend fun test() = "OK"
        
        fun main(): String = TestData.runFunction("test")
    }
""".trimIndent()
    ) {
        classLoader.loadClass("Interface2").run {
            assertHasFunction<String>("test")
            assertHasFunction<Any>("test", Continuation::class.java)
        }
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test")
            assertHasFunction<Any>("test", Continuation::class.java)
        }
        assertFailsWith<NoSuchMethodException> {
            classLoader.loadClass("TestData").getDeclaredMethod("test")
        }
    }

    @Test
    fun `interface codegen`() = testJvmCompile(
        """
        @JvmBlockingBridge
    interface Interface {
        suspend fun test(): String
    }
""", noMain = true
    ) {
        classLoader.loadClass("Interface").run {
            assertHasFunction<String>("test")
        }
    }
}