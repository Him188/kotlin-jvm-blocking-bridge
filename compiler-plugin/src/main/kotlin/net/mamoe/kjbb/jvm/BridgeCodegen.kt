package net.mamoe.kjbb.jvm

import net.mamoe.kjbb.ir.JVM_BLOCKING_BRIDGE_FQ_NAME
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class BridgeCodegen(
    private val codegen: ImplementationBodyCodegen,
    private val generationState: GenerationState = codegen.state
) {
    private inline val v get() = codegen.v
    private inline val clazz get() = codegen.descriptor
    private inline val typeMapper get() = codegen.typeMapper


    fun generate() {
        val members = clazz.unsubstitutedMemberScope
        val names = members.getFunctionNames()
        val functions =
            names.flatMap { members.getContributedFunctions(it, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS) }.toSet()

        for (function in functions) {
            if (function.annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME))
                function.generateBridge()
        }
    }

    fun SimpleFunctionDescriptor.generateBridge() {
        val originFunction = this

        val methodOrigin = OtherOrigin(originFunction)
        val methodName = originFunction.name.asString()
        val methodSignature = originFunction.computeJvmDescriptor(withName = false)

        val mv = v.newMethod(
            OtherOrigin(originFunction),
            Opcodes.ACC_PUBLIC,
            methodName,
            methodSignature,
            null,
            null
        ) // TODO: 2020/7/12 exceptions?

        if (codegen.context.contextKind != OwnerKind.ERASED_INLINE_CLASS && clazz.isInline) {
            FunctionCodegen.generateMethodInsideInlineClassWrapper(
                methodOrigin, this, clazz, mv, typeMapper
            )
            return
        }

        mv.visitAnnotation(Type.getDescriptor(NotNull::class.java), false)?.visitEnd()

        if (!generationState.classBuilderMode.generateBodies) {
            FunctionCodegen.endVisit(mv, methodName, clazz.findPsi())
            return
        }

        val iv = InstructionAdapter(mv)

        mv.visitCode()
        //  AsmUtil.

        //iv.areturn(Type.BOOLEAN_TYPE)

        //v.defineClass()

        FunctionCodegen.endVisit(mv, methodName, clazz.findPsi())
    }
}

fun InstructionAdapter.callRunBlocking() {

}