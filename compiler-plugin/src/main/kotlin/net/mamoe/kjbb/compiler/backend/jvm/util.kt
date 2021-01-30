package net.mamoe.kjbb.compiler.backend.jvm

import net.mamoe.kjbb.compiler.backend.ir.JVM_BLOCKING_BRIDGE_FQ_NAME
import net.mamoe.kjbb.compiler.backend.jvm.HasJvmBlockingBridgeAnnotation.*
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.codegen.state.md5base64
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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

enum class HasJvmBlockingBridgeAnnotation(
    val has: Boolean,
) {
    FROM_FUNCTION(true),
    FROM_CONTAINING_DECLARATION(true),
    NONE(false)
}

fun DeclarationDescriptor.hasJvmBlockingBridgeAnnotation(): HasJvmBlockingBridgeAnnotation {
    return when (this) {
        is ClassDescriptor -> {
            if (this.annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)) FROM_CONTAINING_DECLARATION
            else this.findPackage().hasJvmBlockingBridgeAnnotation()
        }
        is FunctionDescriptor -> {
            if (this.annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)) {
                FROM_FUNCTION
            } else this.containingClass?.hasJvmBlockingBridgeAnnotation() ?: NONE
        }
        is PackageFragmentDescriptor -> {
            if (this.annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)) {
                FROM_CONTAINING_DECLARATION
            } else {
                NONE
            }
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
