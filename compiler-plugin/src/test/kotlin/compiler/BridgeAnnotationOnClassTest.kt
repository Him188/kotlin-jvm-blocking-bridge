package compiler

import assertHasFunction
import assertNoFunction
import createInstance
import org.junit.jupiter.api.Test
import runFunction
import kotlin.coroutines.Continuation
import kotlin.test.assertFailsWith

internal class BridgeAnnotationOnClassTest : AbstractCompilerTest() {
    @Test
    fun `suspend gen`() = testJvmCompile(
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
    fun `non suspend should be ok`() = testJvmCompile(
        """
            @JvmBlockingBridge
            object TestData {
                fun test() {}
            }
        """, noMain = true
    )

    @Test
    fun `effectively public`() = testJvmCompile(
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
    fun `no inspection even inapplicable`() = testJvmCompile(
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
        suspend fun test2(): String
    }
        @JvmBlockingBridge
    interface TestData : Interface3 {
        override suspend fun test() = "OK"
    }
""", noMain = true
    ) {
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test", declaredOnly = true)  // explicit override
            assertNoFunction<String>("test2", declaredOnly = true) // implicit
        }
    }

    @Test
    fun `bridge for interface non-suspend`() = testJvmCompile(
        """
    interface Interface3 {
        suspend fun test(): String
        fun non(): String
    }
        @JvmBlockingBridge
    object TestData : Interface3 {
        override suspend fun test() = "OK"
        override fun non() = "OK"
    }
""", noMain = true
    ) {
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test")
            assertHasFunction<String>("non")
            createInstance().run {
                runFunction<String>("test")
                runFunction<String>("non")
            }
        }
    }

    @Test
    fun `bridge for interface non-suspend complex`() = testJvmCompile(
        """
    @JvmBlockingBridge
    interface Interface3 {
        suspend fun test(): String
        suspend fun implicit(): String = "OK"
        fun non(): String
    }
    @JvmBlockingBridge
    interface Interface4 : Interface3 {
        override suspend fun test(): String
        override fun non(): String
    }
    @JvmBlockingBridge
    object TestData : Interface4 {
        override suspend fun test() = "OK"
        override fun non() = "OK"
    }
    @JvmBlockingBridge
    object TestData2 : Interface4 {
        override suspend fun test() = "OK"
        override suspend fun implicit() = "OK"
        override fun non() = "OK"
    }
""", noMain = true
    ) {
        classLoader.loadClass("Interface3").run {
            assertHasFunction<String>("test", declaredOnly = true)
            assertHasFunction<String>("implicit", declaredOnly = true)
        }
        classLoader.loadClass("Interface4").run {
            assertHasFunction<String>("test", declaredOnly = true)
            assertNoFunction<String>("implicit", declaredOnly = true)
        }
        classLoader.loadClass("TestData").run {
            assertHasFunction<String>("test", declaredOnly = true)
            assertNoFunction<String>("implicit", declaredOnly = true)
        }
        classLoader.loadClass("TestData2").run {
            assertHasFunction<String>("test", declaredOnly = true)
            assertHasFunction<String>("implicit", declaredOnly = true)
        }
    }


    @Test
    fun `bridge for interface implicit override`() = testJvmCompile(
        """
    @JvmBlockingBridge
    interface Interface3 {
        suspend fun test(): String
        suspend fun implicit(): String = "OK"
        fun non(): String
    }
    @JvmBlockingBridge
    interface Interface4 : Interface3 {
        override suspend fun test(): String
        override fun non(): String
    }
""", noMain = true
    ) {
        classLoader.loadClass("Interface3").run {
            assertHasFunction<String>("test", declaredOnly = true)
            assertHasFunction<String>("implicit", declaredOnly = true)
        }
        classLoader.loadClass("Interface4").run {
            assertHasFunction<String>("test", declaredOnly = true)
            assertNoFunction<String>("implicit", declaredOnly = true)
        }
    }

    @Test
    fun `bridge for interface explicit override`() = testJvmCompile(
        """
    @JvmBlockingBridge
    interface Interface3 {
        suspend fun explicit(): String = "OK"
        fun non(): String
    }
    interface Interface4 : Interface3 {
        @JvmBlockingBridge
        override suspend fun explicit(): String
        override fun non(): String
    }
""", noMain = true
    ) {
        classLoader.loadClass("Interface3").run {
            assertHasFunction<String>("explicit", declaredOnly = true)
        }
        classLoader.loadClass("Interface4").run {
            assertHasFunction<String>("explicit", declaredOnly = true)
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