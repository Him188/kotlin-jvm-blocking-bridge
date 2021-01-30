package compiler

import assertHasFunction
import assertNoFunction
import createInstance
import org.junit.jupiter.api.Test
import runFunction
import kotlin.coroutines.Continuation
import kotlin.test.assertFailsWith

internal sealed class BridgeAnnotationOnClassTest(
    ir: Boolean,
) : AbstractCompilerTest(ir) {
    internal class Ir : BridgeAnnotationOnClassTest(ir = true) {
        @Test
        fun test() {
            `effectively public`()
        }
    }

    internal class Jvm : BridgeAnnotationOnClassTest(ir = false) {
        @Test
        fun test() {
            `effectively public`()
        }
    }

    @Test
    open fun `suspend gen`() = testJvmCompile(
        """
            @JvmBlockingBridge
            object TestData {
                suspend fun test() {}
            }
        """, noMain = true
    ) {
        classLoader.loadClass("TestData").kotlin.objectInstance!!.runFunction<Void?>("test")
    }

    @Test
    open fun `non suspend should be ok`() = testJvmCompile(
        """
            @JvmBlockingBridge
            object TestData {
                fun test() {}
            }
        """, noMain = true
    )

    @Test
    open fun `effectively public`() = testJvmCompile(
        """
            @JvmBlockingBridge
            object TestData {
                @PublishedApi
                internal suspend fun test() {}
                internal suspend fun test2() {}
            }
        """, noMain = true
    ) {
        classLoader.loadClass("TestData").run {
            assertHasFunction<Void>("test")
            assertNoFunction<Void>("test2")
        }
    }

    @Test
    open fun `no inspection even inapplicable`() = testJvmCompile(
        """
            @JvmBlockingBridge
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
        @JvmBlockingBridge
    abstract class Abstract {
        abstract suspend fun test(): String
    }
    object TestData : Abstract() {
        override suspend fun test() = "OK"
        
        fun main(): String = TestData.runFunction("test")
    }
"""
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
        @JvmBlockingBridge
    object TestData : Abstract() {
        override suspend fun test() = "OK"
        
        fun main(): String = TestData.runFunction("test")
    }
"""
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
        @JvmBlockingBridge
    object TestData : Interface3 {
        override suspend fun test() = "OK"
    }
""", noMain = true
    ) {
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test")
            createInstance().run {
                runFunction<String>("test")
            }
        }
    }

    @Test
    open fun `bridge for interface inheritance`() = testJvmCompile(
        """
        @JvmBlockingBridge
    interface Interface2 {
        suspend fun test(): String
    }
    object TestData : Interface2 {
        override suspend fun test() = "OK"
        
        fun main(): String = TestData.runFunction("test")
    }
"""
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