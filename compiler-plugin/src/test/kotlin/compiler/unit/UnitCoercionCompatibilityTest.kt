@file:Suppress("RedundantSuspendModifier")

package compiler.unit

import assertHasFunction
import assertNoFunction
import net.mamoe.kjbb.compiler.JvmBlockingBridgeCompilerConfigurationKeys
import net.mamoe.kjbb.compiler.UnitCoercion
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.Test
import runFunction
import kotlin.test.assertTrue

internal sealed class UnitCoercionCompatibilityTest(ir: Boolean) : AbstractUnitCoercionTest(ir) {
    override val overrideCompilerConfiguration: CompilerConfiguration = CompilerConfiguration().apply {
        put(JvmBlockingBridgeCompilerConfigurationKeys.UNIT_COERCION, UnitCoercion.COMPATIBILITY.toString())
    }

    // class Ir : UnitCoercionCompatibilityTest(true) // IR is correct in both old and new, no need to test compatibility.

    class Jvm : UnitCoercionCompatibilityTest(false) {

        @Test
        fun test() {
            `fake override comp`()
        }
    }

    @Test
    fun `fake override comp`() = testJvmCompile("""
        interface ATestData : Inter {
            @JvmBlockingBridge override suspend fun test2(arg: String)
        }
        interface Inter { 
            @JvmBlockingBridge suspend fun test(arg: String)
            @JvmBlockingBridge suspend fun test2(arg: String)
        }
    """, noMain = true
    ) {
        classLoader.loadClass("Inter").apply {
            assertHasFunction<Void>("test2", String::class.java)
            assertHasFunction<Unit>("test2", String::class.java)
            assertHasFunction<Void>("test", String::class.java)
            assertHasFunction<Unit>("test", String::class.java)
        }
    }

    @Test
    fun `member comp`() = testJvmCompile(
        """
    class TestData {
        @JvmBlockingBridge
        suspend fun test(arg: String) { // returns Unit
        }
        
        companion object {
            @JvmStatic
            fun main(): String {
                Class.forName("TestData").assertHasFunction<Void>("test", String::class.java) 
                Class.forName("TestData").assertHasFunction<Unit>("test", String::class.java)
                return "OK"
            }
        }
    }
"""
    )

    @Test
    fun `jvm overloads comp`() = testJvmCompile(
        """
    class TestData {
        @JvmOverloads
        @JvmBlockingBridge
        suspend fun test(b: Boolean = true, arg: String = "") { // returns Unit
        }
    }
""", noMain = true
    ) {
        classLoader.loadClass("TestData").run {
            assertHasFunction<Void>("test", Boolean::class.javaPrimitiveType!!, String::class.java)
            assertHasFunction<Void>("test", Boolean::class.javaPrimitiveType!!)
            assertHasFunction<Void>("test")

            assertHasFunction<Unit>("test", Boolean::class.javaPrimitiveType!!, String::class.java) {
                assertTrue(isSynthetic, "unit compatibility bridge is not synthetic")
            }
            assertNoFunction<Unit>("test", Boolean::class.javaPrimitiveType!!)
            assertNoFunction<Unit>("test")

            this.getConstructor().newInstance().runFunction<Any?>("test", true, "")
            this.getConstructor().newInstance().runFunction<Any?>("test", true)
        }
    }

    @Test
    fun `static comp`() = testJvmCompile(
        """
    object TestData {
        @JvmStatic @JvmBlockingBridge
        suspend fun test(arg: String) { // returns Unit
        }
        
        fun main(): String {
            Class.forName("TestData").assertHasFunction<Void>("test", String::class.java) 
            Class.forName("TestData").assertHasFunction<Unit>("test", String::class.java) 
            return "OK"
        }
    }
"""
    )

    @Test
    fun `static companion comp`() = testJvmCompile(
        """
    class TestData {
        companion object {
            @JvmStatic @JvmBlockingBridge
            suspend fun test(arg: String) { // returns Unit
            }
            
            @JvmStatic
            fun main(): String {
                Class.forName("TestData").assertHasFunction<Void>("test", String::class.java) 
                // Class.forName("TestData").assertHasFunction<Unit>("test", String::class.java) // as expected
                Class.forName("TestData\${'$'}Companion").assertHasFunction<Void>("test", String::class.java) 
                Class.forName("TestData\${'$'}Companion").assertHasFunction<Unit>("test", String::class.java) 
                return "OK"
            }
        }
    }
"""
    )
}