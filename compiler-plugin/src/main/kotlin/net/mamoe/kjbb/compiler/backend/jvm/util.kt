package net.mamoe.kjbb.compiler.backend.jvm

import net.mamoe.kjbb.compiler.backend.ir.JVM_BLOCKING_BRIDGE_FQ_NAME
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
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

fun DeclarationDescriptor.hasJvmBlockingBridgeAnnotation(): Boolean = this.annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)