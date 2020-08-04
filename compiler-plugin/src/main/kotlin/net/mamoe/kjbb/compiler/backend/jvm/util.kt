package net.mamoe.kjbb.compiler.backend.jvm

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.FunctionGenerationStrategy
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter


internal fun ImplementationBodyCodegen.generateMethod(
    function: FunctionDescriptor,
    block: InstructionAdapter.(JvmMethodSignature, ExpressionCodegen) -> Unit
) {
    this.functionCodegen.generateMethod(
        OtherOrigin(this.myClass.psiOrParent, function), function,
        object : FunctionGenerationStrategy.CodegenBased(this.state) {
            override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                codegen.v.block(signature, codegen)
            }
        })
}


val FunctionDescriptor.jvmName: String?
    get() = annotations.findAnnotation(JVM_NAME_FQ_NAME)
        ?.argumentValue("name")
        ?.value as String?

val FunctionDescriptor.jvmNameOrName: Name
    get() = jvmName?.let { Name.identifier(it) } ?: name

private val JVM_NAME_FQ_NAME = FqName(JvmName::class.qualifiedName!!)

/**
 * For ignoring
 */
object GeneratedBlockingBridgeStubForResolution : CallableDescriptor.UserDataKey<Boolean>

fun FunctionDescriptor.isGeneratedBlockingBridgeStub(): Boolean =
    this.getUserData(GeneratedBlockingBridgeStubForResolution) == true