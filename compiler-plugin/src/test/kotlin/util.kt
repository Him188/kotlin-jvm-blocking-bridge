@file:Suppress("UNCHECKED_CAST", "unused")

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import net.mamoe.kjbb.JvmBlockingBridge
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.companionObjectInstance
import kotlin.test.assertEquals

// Expose to top-package for TestData
typealias JvmBlockingBridge = JvmBlockingBridge

fun <R> Any.runFunction(name: String, vararg args: Any): R {
    return this::class.java.getMethod(name, *args.map { it::class.javaPrimitiveType ?: it::class.java }.toTypedArray())
        .invoke(this, *args) as R
}

fun <R> Class<*>.runStaticFunction(name: String, vararg args: Any): R {
    return getMethod(name, *args.map { it::class.java }.toTypedArray()).also {
        assert(Modifier.isStatic(it.modifiers)) { "method $name is not static" }
    }.invoke(null, *args)!! as R
}

fun compile(
    @Language("kt")
    source: String,
    ir: Boolean,
    jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
) = compile(source, null, ir, jvmTarget)

fun compile(
    @Language("kt")
    source: String,
    @Language("java")
    java: String? = null,
    ir: Boolean,
    jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
): KotlinCompilation.Result {
    val intrinsicImports = listOf(
        "import kotlin.test.*",
        "import JvmBlockingBridge"
    )

    val kotlinSource = if (source.trim().startsWith("package")) {
        SourceFile.kotlin("TestData.kt", run {
            source.trimIndent().lines().mapTo(LinkedList()) { it }
                .apply { addAll(1, intrinsicImports) }
                .joinToString("\n")
        })
    } else {
        SourceFile.kotlin(
            "TestData.kt", "${intrinsicImports.joinToString("\n")}\n${source.trimIndent()}"
        )
    }

    return KotlinCompilation().apply {
        sources = listOfNotNull(
            kotlinSource,
            java?.let { javaSource ->
                SourceFile.java(
                    Regex("""class\s*(.*?)\s*\{""").find(javaSource)!!.groupValues[1].let { "$it.java" },
                    javaSource
                )
            }
        )

        compilerPlugins = listOf(TestComponentRegistrar())
        verbose = false

        this.jvmTarget = jvmTarget.description

        workingDir = File("testCompileOutput").apply {
            this.walk().forEach { it.delete() }
            mkdir()
        }

        useIR = ir

        inheritClassPath = true
        messageOutputStream = System.out
    }.compile().also { result ->
        assert(result.exitCode == KotlinCompilation.ExitCode.OK)
    }
}


fun testIrCompile(
    @Language("kt")
    kt: String,
    @Language("java")
    java: String? = null,
    noMain: Boolean = false,
    ir: Boolean = true,
    jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
    block: KotlinCompilation.Result.() -> Unit = {},
) = testJvmCompile(kt, java, noMain, ir, jvmTarget, block)


fun testJvmCompile(
    @Language("kt")
    kt: String,
    @Language("java")
    java: String? = null,
    noMain: Boolean = false,
    ir: Boolean = false,
    jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
    block: KotlinCompilation.Result.() -> Unit = {},
) {
    val result = compile(kt, java, ir, jvmTarget)

    if (!noMain) {
        @Suppress("UNCHECKED_CAST")
        val test = result.classLoader.loadClass("TestData")
        assertEquals(
            "OK",
            listOfNotNull(
                test.kotlin.objectInstance, test.kotlin.companionObjectInstance, test.kotlin.createInstanceOrNull()
            ).associateWith { obj ->
                obj::class.java.methods.find { it.name == "main" }
            }.entries.find { it.value != null }?.let { (instance, method) ->
                method!!.invoke(instance)
            } as String? ?: error("Cannot find a `main`"))
    }
    block(result)
}


@SinceKotlin("1.1")
fun <T : Any> KClass<T>.createInstanceOrNull(): T? {
    // TODO: throw a meaningful exception
    val noArgsConstructor = constructors.singleOrNull { it.parameters.all(KParameter::isOptional) }
        ?: return null

    return noArgsConstructor.callBy(emptyMap())
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