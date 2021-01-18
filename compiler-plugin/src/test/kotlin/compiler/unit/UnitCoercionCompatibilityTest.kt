package compiler.unit

import net.mamoe.kjbb.compiler.JvmBlockingBridgeCompilerConfigurationKeys
import net.mamoe.kjbb.compiler.UnitCoercion
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.Test

internal sealed class UnitCoercionCompatibilityTest(ir: Boolean) : AbstractUnitCoercionTest(ir) {
    override val overrideCompilerConfiguration: CompilerConfiguration = CompilerConfiguration().apply {
        put(JvmBlockingBridgeCompilerConfigurationKeys.UNIT_COERCION, UnitCoercion.COMPATIBILITY.toString())
    }

    // class Ir : UnitCoercionCompatibilityTest(true) // IR is correct in both old and new, no need to test compatibility.

    class Jvm : UnitCoercionCompatibilityTest(false) {

        @Test
        fun test() {
            `static companion comp`()
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