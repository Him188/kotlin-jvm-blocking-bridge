package net.mamoe.kjbb.jvm

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.FunctionGenerationStrategy
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
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
