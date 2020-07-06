import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import net.mamoe.kjbb.JvmBlockingBridgeComponentRegistrar
import org.junit.jupiter.api.Test
import java.io.File

class TestEnvClass

class TestCompiling {

    @Test
    fun `test my annotation processor`() {
        val kotlinSource = SourceFile.kotlin(
            "KClass.kt", """
        object KClass {
            @net.mamoe.kjbb.JvmBlockingBridge
            suspend fun test(){
            this
            }
        
            fun foo() {
                // Classes from the test environment are visible to the compiled sources
                val testEnvClass = TestEnvClass()
            }
        }
    """
        )

        val javaSource = SourceFile.java(
            "JClass.java", """
        public class JClass {
            public void bar() {
                // compiled Kotlin classes are visible to Java sources
                KClass kClass = new KClass();
            }
        }
    """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(kotlinSource, javaSource)

            compilerPlugins = listOf(JvmBlockingBridgeComponentRegistrar())
            verbose = false

            workingDir = File("testCompileOutput").apply { mkdir() }

            useIR = true

            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()

    }
}