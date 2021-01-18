@file:Suppress("UNCHECKED_CAST", "unused")

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import net.mamoe.kjbb.JvmBlockingBridge
import net.mamoe.kjbb.compiler.extensions.JvmBlockingBridgeComponentRegistrar
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.CompilerConfiguration
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

fun Class<*>.getFunctionReturnType(name: String, vararg args: Any): String {
    return getMethod(name, *args.map { it::class.java }.toTypedArray()).returnType.canonicalName
}

inline fun <reified R : Any> Class<*>.assertHasFunction(name: String, vararg args: Class<*>) {
    return assertHasFunction(name, args = args, returnType = R::class.javaPrimitiveType ?: R::class.java)
}

fun Class<*>.assertHasFunction(name: String, vararg args: Class<*>, returnType: Class<*>) {
    val any = declaredMethods.any {
        it.returnType == returnType &&
                it.parameterCount == args.size &&
                it.parameters.zip(args).all { (param, clazz) -> param.type == clazz }
    }
    if (!any)
        throw AssertionError("Class does not has method $name(${args.joinToString { it.canonicalName }})${returnType.canonicalName}")
}

fun compile(
    @Language("kt")
    source: String,
    ir: Boolean,
    jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
    overrideCompilerConfiguration: CompilerConfiguration? = null,
    config: KotlinCompilation.() -> Unit = {},
) = compile(source,
    null,
    ir,
    jvmTarget,
    overrideCompilerConfiguration = overrideCompilerConfiguration,
    config = config)

fun compile(
    @Language("kt")
    source: String,
    @Language("java")
    java: String? = null,
    ir: Boolean,
    jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
    overrideCompilerConfiguration: CompilerConfiguration? = null,
    config: KotlinCompilation.() -> Unit = {},
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

        compilerPlugins = listOf(JvmBlockingBridgeComponentRegistrar(overrideCompilerConfiguration))
        verbose = false

        this.jvmTarget = jvmTarget.description

        workingDir = File("testCompileOutput").apply {
            this.walk().forEach { it.delete() }
            mkdir()
        }

        useIR = ir

        inheritClassPath = true
        messageOutputStream = System.out

        config()
    }.compile().also { result ->
        assert(result.exitCode == KotlinCompilation.ExitCode.OK) {
            "Test data compilation failed."
        }
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
    overrideCompilerConfiguration: CompilerConfiguration? = null,
    config: KotlinCompilation.() -> Unit = {},
    block: KotlinCompilation.Result.() -> Unit = {},
) = testJvmCompile(kt, java, noMain, ir, jvmTarget, overrideCompilerConfiguration, config, block)


fun testJvmCompile(
    @Language("kt")
    kt: String,
    @Language("java")
    java: String? = null,
    noMain: Boolean = false,
    ir: Boolean = false,
    jvmTarget: JvmTarget = JvmTarget.JVM_1_8,
    overrideCompilerConfiguration: CompilerConfiguration? = null,
    config: KotlinCompilation.() -> Unit = {},
    block: KotlinCompilation.Result.() -> Unit = {},
) {
    val result =
        compile(kt,
            java,
            ir,
            jvmTarget,
            overrideCompilerConfiguration = overrideCompilerConfiguration,
            config = config)

    if (!noMain) {
        @Suppress("UNCHECKED_CAST")
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


@SinceKotlin("1.1")
fun <T : Any> KClass<T>.createInstanceOrNull(): T? {
    // TODO: throw a meaningful exception
    val noArgsConstructor = constructors.singleOrNull { it.parameters.all(KParameter::isOptional) }
        ?: return null

    return noArgsConstructor.callBy(emptyMap())
}

internal
val Method.visibility: Visibility
    get() = when {
        Modifier.isPublic(this.modifiers) -> Visibilities.Public
        Modifier.isPrivate(this.modifiers) -> Visibilities.Private
        Modifier.isProtected(this.modifiers) -> Visibilities.Protected
        else -> Visibilities.PrivateToThis
    }

internal
val Method.modality: Modality
    get() = when {
        Modifier.isFinal(this.modifiers) -> Modality.FINAL
        Modifier.isAbstract(this.modifiers) -> Modality.ABSTRACT
        else -> Modality.OPEN
    }