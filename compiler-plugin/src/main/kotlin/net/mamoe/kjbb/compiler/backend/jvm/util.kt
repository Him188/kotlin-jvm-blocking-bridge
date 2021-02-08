package net.mamoe.kjbb.compiler.backend.jvm

import net.mamoe.kjbb.compiler.backend.ir.JVM_BLOCKING_BRIDGE_FQ_NAME
import net.mamoe.kjbb.compiler.backend.jvm.HasJvmBlockingBridgeAnnotation.*
import org.jetbrains.kotlin.codegen.state.md5base64
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.annotations.argumentValue

internal val FunctionDescriptor.jvmName: String?
    get() = annotations.findAnnotation(JVM_NAME_FQ_NAME)
        ?.argumentValue("name")
        ?.value as String?

internal val FunctionDescriptor.jvmNameOrName: Name
    get() = jvmName?.let { Name.identifier(it) } ?: name

private val JVM_NAME_FQ_NAME = FqName(JvmName::class.qualifiedName!!)

/**
 * For ignoring
 */
object GeneratedBlockingBridgeStubForResolution : CallableDescriptor.UserDataKey<Boolean>

fun FunctionDescriptor.isGeneratedBlockingBridgeStub(): Boolean =
    this.getUserData(GeneratedBlockingBridgeStubForResolution) == true

fun FunctionDescriptor.findOverriddenDescriptorsHierarchically(filter: (FunctionDescriptor) -> Boolean): FunctionDescriptor? {
    val overridden = this.overriddenDescriptors
    for (overriddenDescriptor in overridden) {
        if (filter(overriddenDescriptor)) {
            return overriddenDescriptor
        }
    }
    for (overriddenDescriptor in overridden) {
        val got = overriddenDescriptor.findOverriddenDescriptorsHierarchically(filter)
        if (got != null)
            return got
    }
    return null
}

enum class HasJvmBlockingBridgeAnnotation(
    val has: Boolean,
) {
    FROM_FUNCTION(true),

    /**
     * @since 1.8
     */
    FROM_CONTAINING_DECLARATION(true),

    /**
     * @since 1.10
     */
    ENABLE_FOR_MODULE(true),
    NONE(false),
}

fun DeclarationDescriptor.hasJvmBlockingBridgeAnnotation(
    bindingContext: BindingContext,
    enableForModule: Boolean,
): HasJvmBlockingBridgeAnnotation {
    return when (this) {
        is ClassDescriptor -> {
            when {
                enableForModule -> ENABLE_FOR_MODULE
                this.annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME) -> FROM_CONTAINING_DECLARATION
                findFileAnnotation(bindingContext, JVM_BLOCKING_BRIDGE_FQ_NAME) != null -> FROM_CONTAINING_DECLARATION
                else -> NONE
            }
        }
        is FunctionDescriptor -> {
            if (this.annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)) {
                FROM_FUNCTION
            } else this.containingClass?.hasJvmBlockingBridgeAnnotation(bindingContext, enableForModule) ?: NONE
        }
        is PackageFragmentDescriptor -> {
            if (findFileAnnotation(bindingContext, JVM_BLOCKING_BRIDGE_FQ_NAME) != null) {
                FROM_CONTAINING_DECLARATION
            } else NONE
        }
        else -> NONE
    }
}

internal fun FunctionDescriptor.mangleBridgeLambdaClassname(
    parentName: String = this.containingDeclaration.name.identifier,
): String {
    val signature = md5base64(this.computeJvmDescriptor(withReturnType = true, withName = true))
    return "$parentName\$\$$name\$\$bb\$$signature" // clazz$$functionName$$bb$6sv54r
}
