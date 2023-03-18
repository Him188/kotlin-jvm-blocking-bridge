@file:Suppress("UNCHECKED_CAST", "unused")

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
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
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.companionObjectInstance
import kotlin.test.assertEquals

// Expose to top-package for TestData
typealias JvmBlockingBridge = JvmBlockingBridge

fun <T : Any> Class<T>.createInstance(): T {
    return kotlin.objectInstance ?: kotlin.createInstanceOrNull() ?: getConstructor().newInstance()
}

@Deprecated(
    "runFunction on class is an error.",
    replaceWith = ReplaceWith("createInstance().run { \n runFunction<R>(name)\n }"),
    level = DeprecationLevel.ERROR
)
fun <R> Class<*>.runFunction(name: String): Nothing {
    error("runFunction on class is an error.")
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated(
    "runFunction on class is an error.",
    replaceWith = ReplaceWith("createInstance().run { \n runFunction<R>(name, args)\n }"),
    level = DeprecationLevel.ERROR
)
inline fun <reified R> Class<*>.runFunction(name: String, vararg args: Any): Nothing {
    error("runFunction on class is an error.")
}

fun <R> Any.runFunction(name: String, vararg args: Any): R {
    return this::class.java.getMethod(name, *args.map { it::class.javaPrimitiveType ?: it::class.java }.toTypedArray())
        .invoke(this, *args) as R
}

fun <R> Class<*>.runStaticFunction(name: String, vararg args: Any): R {
    return getMethod(name, *args.map { it::class.javaPrimitiveType ?: it::class.java }.toTypedArray()).also {
        assert(Modifier.isStatic(it.modifiers)) { "method $name is not static" }
    }.invoke(null, *args)!! as R
}

fun Class<*>.getFunctionReturnType(name: String, vararg args: Class<*>): String {
    return getMethod(name, *args).returnType.canonicalName
}

inline fun <reified R : Any> Class<*>.assertHasFunction(
    name: String,
    vararg args: Class<*>,
    declaredOnly: Boolean = false,
    noinline runIfFound: Method.() -> Unit = {},
) {
    return assertHasFunction(
        name,
        args = args,
        returnType = R::class.javaPrimitiveType ?: R::class.java,
        declaredOnly,
        runIfFound = runIfFound
    )
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
inline fun <reified R : Any> Class<*>.assertHasFunction(
    name: String,
    vararg args: KClass<*>,
    declaredOnly: Boolean = false,
    noinline runIfFound: Method.() -> Unit = {},
) {
    return assertHasFunction(
        name,
        args = args.map { it.javaPrimitiveType ?: it.java }.toTypedArray(),
        returnType = R::class.javaPrimitiveType ?: R::class.java,
        declaredOnly,
        runIfFound = runIfFound
    )
}

inline fun <reified R : Any> Class<*>.assertNoFunction(
    name: String,
    vararg args: Class<*>,
    declaredOnly: Boolean = false,
) {
    return assertNoFunction(
        name,
        args = args,
        returnType = R::class.javaPrimitiveType ?: R::class.java,
        declaredOnly = declaredOnly
    )
}

inline fun <reified R : Any> Class<*>.getFunctionWithReturnType(name: String, vararg args: Class<*>): Method {
    val returnType = R::class.javaPrimitiveType ?: R::class.java
    val ret = allMethods.find {
        it.name == name &&
                it.returnType == returnType &&
                it.parameterCount == args.size &&
                it.parameters.zip(args).all { (param, clazz) -> param.type == clazz }
    }

    return ret
        ?: throw AssertionError(
            "Class '${this.name}' does not have method $name(${args.joinToString { it.canonicalName }})${returnType.canonicalName}. All methods list: " +
                    "\n${allMethods.joinToString("\n")}\n"
        )
}

val Class<*>.allMethods: Set<Method>
    get() {
        fun Class<*>?.shouldInclude(): Boolean {
            if (this == null) return false
            return !this.`package`.name.startsWith("java")
                    && !this.`package`.name.startsWith("kotlin")
        }

        val set = declaredMethods.toMutableSet()
        set += superclass.takeIf { it.shouldInclude() }?.allMethods.orEmpty()
        set += interfaces.flatMap {
            it.takeIf { it.shouldInclude() }?.allMethods.orEmpty()
        }

        return set
    }

fun Class<*>.assertHasFunction(
    name: String,
    vararg args: Class<*>,
    returnType: Class<*>,
    declaredOnly: Boolean = false,
    runIfFound: Method.() -> Unit,
) {
    val any = (if (declaredOnly) declaredMethods.toSet() else allMethods).find {
        it.name == name &&
                it.returnType == returnType &&
                it.parameterCount == args.size &&
                it.parameters.zip(args).all { (param, clazz) -> param.type == clazz }
    }
        ?: throw AssertionError(
            "Class '${this.name}' does not have method $name(${args.joinToString { it.canonicalName }})${returnType.canonicalName}. All methods list: " +
                    "\n${allMethods.joinToString("\n")}\n"
        )

    runIfFound(any)
}

fun Class<*>.assertNoFunction(
    name: String,
    vararg args: Class<*>,
    returnType: Class<*>,
    declaredOnly: Boolean = false,
) {
    val any = (if (declaredOnly) declaredMethods.toSet() else allMethods).any {
        it.name == name &&
                it.returnType == returnType &&
                it.parameterCount == args.size &&
                it.parameters.zip(args).all { (param, clazz) -> param.type == clazz }
    }
    if (any)
        throw AssertionError("Class '${this.name}' does has method $name(${args.joinToString { it.canonicalName }})${returnType.canonicalName}")
}


@SinceKotlin("1.1")
fun <T : Any> KClass<T>.createInstanceOrNull(): T? {
    // TODO: throw a meaningful exception
    val noArgsConstructor = constructors.singleOrNull { it.parameters.all(KParameter::isOptional) }
        ?: return null

    return noArgsConstructor.callBy(emptyMap())
}
