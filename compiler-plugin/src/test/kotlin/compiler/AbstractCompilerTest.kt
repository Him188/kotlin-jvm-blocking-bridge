package compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import createInstanceOrNull
import me.him188.kotlin.jvm.blocking.bridge.compiler.extensions.BridgeComponentRegistrar
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import kotlin.reflect.full.companionObjectInstance
import kotlin.test.assertEquals

internal abstract class AbstractCompilerTest {
    protected open val overrideCompilerConfiguration: CompilerConfiguration? = null

    companion object {
        const val FILE_SPLITTER = "-------------------------------------"
    }


    fun compile(
        @Language("kt")
        source: String,
        jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
        overrideCompilerConfiguration: CompilerConfiguration? = this.overrideCompilerConfiguration,
        config: KotlinCompilation.() -> Unit = {},
    ) = compile(
        source,
        null,
        jvmTarget,
        overrideCompilerConfiguration = overrideCompilerConfiguration,
        config = config
    )


    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        @Language("kt")
        sources: String,
        @Language("java")
        java: String? = null,
        jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
        overrideCompilerConfiguration: CompilerConfiguration? = this.overrideCompilerConfiguration,
        config: KotlinCompilation.() -> Unit = {},
    ): KotlinCompilation.Result {
        val intrinsicImports = listOf(
            "import kotlin.test.*",
            "import JvmBlockingBridge"
        )

        val kotlinSources = sources.split(FILE_SPLITTER).mapIndexed { index, source ->
            when {
                source.trim().startsWith("package") -> {
                    SourceFile.kotlin("TestData${index}.kt", run {
                        source.trimIndent().lines().mapTo(LinkedList()) { it }
                            .apply { addAll(1, intrinsicImports) }
                            .joinToString("\n")
                    })
                }

                source.trim().startsWith("@file:") -> {
                    SourceFile.kotlin("TestData${index}.kt", run {
                        source.trim().trimIndent().lines().mapTo(LinkedList()) { it }
                            .apply { addAll(1, intrinsicImports) }
                            .joinToString("\n")
                    })
                }

                else -> {
                    SourceFile.kotlin(
                        name = "TestData${index}.kt",
                        contents = "${intrinsicImports.joinToString("\n")}\n${source.trimIndent()}"
                    )
                }
            }
        }


        return KotlinCompilation().apply {
            this.sources = listOfNotNull(
                *kotlinSources.toTypedArray(),
                java?.let { javaSource ->
                    SourceFile.java(
                        Regex("""class\s*(.*?)\s*\{""").find(javaSource)!!.groupValues[1].let { "$it.java" },
                        javaSource
                    )
                }
            )

            compilerPluginRegistrars = listOf(BridgeComponentRegistrar(overrideCompilerConfiguration))
            verbose = false

            this.jvmTarget = jvmTarget.description

            workingDir = File("testCompileOutput").apply {
                this.walk().forEach { it.delete() }
                mkdir()
            }

            useIR = true

            inheritClassPath = true
            messageOutputStream = System.out

            config()
        }.compile().also { result ->
            assert(result.exitCode == KotlinCompilation.ExitCode.OK) {
                "Test data compilation failed."
            }
        }
    }


    fun testJvmCompile(
        @Language("kt")
        kt: String,
        @Language("java")
        java: String? = null,
        noMain: Boolean = false,
        jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
        overrideCompilerConfiguration: CompilerConfiguration? = this.overrideCompilerConfiguration,
        config: KotlinCompilation.() -> Unit = {},
        block: KotlinCompilation.Result.() -> Unit = {},
    ) {
        val result =
            compile(
                kt,
                java,
                jvmTarget,
                overrideCompilerConfiguration = overrideCompilerConfiguration,
                config = config
            )

        if (!noMain) {
            val test = result.classLoader.loadClass("TestData")
            assertEquals(
                "OK",
                listOfNotNull(
                    test.kotlin.objectInstance,
                    test.kotlin.companionObjectInstance,
                    test.kotlin.createInstanceOrNull()
                ).associateWith { obj ->
                    obj::class.java.methods.find { it.name == "main" }
                }.entries.find { it.value != null }?.let { (instance, method) ->
                    method!!.invoke(instance)
                } as String? ?: error("Cannot find a `main`"))
        }
        block(result)
    }


    internal val Method.visibility: Visibility
        get() = when {
            Modifier.isPublic(this.modifiers) -> Visibilities.Public
            Modifier.isPrivate(this.modifiers) -> Visibilities.Private
            Modifier.isProtected(this.modifiers) -> Visibilities.Protected
            else -> Visibilities.PrivateToThis
        }

    internal val Method.modality: Modality
        get() = when {
            Modifier.isFinal(this.modifiers) -> Modality.FINAL
            Modifier.isAbstract(this.modifiers) -> Modality.ABSTRACT
            else -> Modality.OPEN
        }


}