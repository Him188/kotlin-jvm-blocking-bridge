package net.mamoe.kjbb.jvm

import net.mamoe.kjbb.ir.GENERATED_BLOCKING_BRIDGE_FQ_NAME
import net.mamoe.kjbb.ir.JVM_BLOCKING_BRIDGE_FQ_NAME
import net.mamoe.kjbb.ir.identifierOrMappedSpecialName
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.coroutines.continuationAsmType
import org.jetbrains.kotlin.codegen.inline.NUMBERED_FUNCTION_PREFIX
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import kotlin.reflect.KClass

interface TypeMapperAware {
    val typeMapper: KotlinTypeMapper

    fun KotlinType.asmType(): Type = this.asmType(typeMapper)
}

val GENERATED_BLOCKING_BRIDGE_ASM_TYPE = GENERATED_BLOCKING_BRIDGE_FQ_NAME.topLevelClassAsmType()
val JVM_BLOCKING_BRIDGE_ASM_TYPE = JVM_BLOCKING_BRIDGE_FQ_NAME.topLevelClassAsmType()

class BridgeCodegen(
    private val codegen: ImplementationBodyCodegen,
    private val generationState: GenerationState = codegen.state
) : TypeMapperAware {
    private inline val v get() = codegen.v
    private inline val clazz get() = codegen.descriptor
    override val typeMapper: KotlinTypeMapper get() = codegen.typeMapper


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
            ACC_PUBLIC,
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


        fun MethodVisitor.genAnnotation(descriptor: String, visible: Boolean) {
            visitAnnotation(descriptor, visible)?.visitEnd()
        }

        mv.genAnnotation(
            descriptor =
            if (originFunction.returnTypeOrNothing.isNullable())
                Nullable::class.asmTypeDescriptor
            else NotNull::class.asmTypeDescriptor,
            visible = false
        )

        originFunction.annotations
            .filterNot { it.type.asmType().descriptor == GENERATED_BLOCKING_BRIDGE_ASM_TYPE.descriptor }
            .filterNot { it.type.asmType().descriptor == JVM_BLOCKING_BRIDGE_ASM_TYPE.descriptor }
            .forEach { annotationDescriptor ->
                mv.genAnnotation(annotationDescriptor.type.asmType().descriptor, true)
            }

        mv.visitAnnotation(GENERATED_BLOCKING_BRIDGE_ASM_TYPE.descriptor, true)?.visitEnd()

        if (!generationState.classBuilderMode.generateBodies) {
            FunctionCodegen.endVisit(mv, methodName, clazz.findPsi())
            return
        }

        val iv = InstructionAdapter(mv)

        val lambdaClassDescriptor = generateLambdaForRunBlocking(
            originFunction, codegen.state,
            originFunction.findPsi()!!,
            codegen.parentCodegen?.v?.thisName ?: codegen.v.thisName, // TODO: 2020/7/15 check name
            codegen.v.thisName
        ).let { Type.getObjectType(it) }

        mv.visitCode()
        //  AsmUtil.

        with(iv) {
            aconst(null) // coroutineContext

            // call lambdaConstructor
            mv.visitTypeInsn(NEW, lambdaClassDescriptor.internalName)
            mv.visitInsn(DUP)

            mv.visitVarInsn(ALOAD, 0)
            // load(0, lambdaClassDescriptor)
            for ((index, parameter) in originFunction.allParameters.drop(1).withIndex()) {
                load(index + 1, parameter.type.asmType())
            }

            invokespecial(
                lambdaClassDescriptor.internalName,
                "<init>",
                originFunction.allParameters.computeJvmDescriptorForMethod(typeMapper, "V"),
                false
            )

            aconst(1) // $default params flag
            aconst(null) // $default marker
            // public fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T {
            invokestatic(
                "kotlinx/coroutines/BuildersKt",
                "runBlocking\$default",
                "(Lkotlin/coroutines/CoroutineContext;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)Ljava/lang/Object;",
                false
            )
            //cast(Type.getType(Any::class.java), originFunction.returnTypeOrNothing.asmType())
            //

            if (originFunction.returnType != null) {
                if (originFunction.returnType?.asmType() == Type.getType(Unit::class.java)) {
                    mv.visitInsn(RETURN)
                } else {
                    mv.visitTypeInsn(CHECKCAST, originFunction.returnType!!.asmType().internalName)
                    areturn(originFunction.returnType!!.asmType())
                }
            }
        }
        //iv.areturn(Type.BOOLEAN_TYPE)

        FunctionCodegen.endVisit(mv, methodName, clazz.findPsi())
    }
}

fun InstructionAdapter.callRunBlocking() {

}

/**
 * @see Type.getDescriptor
 */
internal val Class<*>.asmTypeDescriptor: String
    get() = Type.getDescriptor(this)

/**
 * @see Type.getDescriptor
 */
internal val KClass<*>.asmTypeDescriptor: String
    get() = Type.getDescriptor(this.java)

internal fun List<ParameterDescriptor>.computeJvmDescriptorForMethod(
    typeMapper: KotlinTypeMapper,
    returnTypeDescriptor: String
): String {
    return "(${this.joinToString("") { it.type.asmType(typeMapper).descriptor }})$returnTypeDescriptor"
}

private fun TypeMapperAware.generateLambdaForRunBlocking(
    originFunction: FunctionDescriptor,
    state: GenerationState,
    originElement: PsiElement,
    packagePartClassInternalName: String,
    parentName: String
): String {
    val allParameters = originFunction.allParameters // dispatch + extension + value

    val internalName = "$parentName$$${originFunction.name}\$blocking_bridge"
    val lambdaBuilder = state.factory.newVisitor(
        OtherOrigin(originFunction),
        Type.getObjectType(internalName),
        originElement.containingFile
    )

    lambdaBuilder.defineClass(
        originElement, state.classFileVersion,
        ACC_FINAL or ACC_SUPER or ACC_SYNTHETIC,
        internalName, null,
        AsmTypes.LAMBDA.internalName,
        arrayOf(NUMBERED_FUNCTION_PREFIX + "2") // Function2<in P1, in P2, out R>
    )

    for (valueParameter in allParameters) {
        lambdaBuilder.newField(
            JvmDeclarationOrigin.NO_ORIGIN,
            ACC_PRIVATE or ACC_FINAL,
            valueParameter.name.identifierOrMappedSpecialName,
            valueParameter.type.asmType().descriptor, null, null
        )
    }

    lambdaBuilder.newMethod(
        JvmDeclarationOrigin.NO_ORIGIN,
        AsmUtil.NO_FLAG_PACKAGE_PRIVATE or ACC_SYNTHETIC,
        "<init>",
        allParameters.computeJvmDescriptorForMethod(typeMapper, "V"), null, null
    ).apply {
        visitCode()

        // super(2)
        visitVarInsn(ALOAD, 0)
        visitInsn(ICONST_2) // Function2
        visitMethodInsn(
            INVOKESPECIAL,
            AsmTypes.LAMBDA.internalName,
            "<init>",
            Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE),
            false
        )

        // set fields
        for ((index, valueParameter) in allParameters.withIndex()) {
            visitVarInsn(ALOAD, 0)
            visitVarInsn(ALOAD, 1 + index)
            visitFieldInsn(
                PUTFIELD,
                lambdaBuilder.thisName,
                valueParameter.name.identifierOrMappedSpecialName,
                valueParameter.type.asmType().descriptor
            )
        }

        visitInsn(RETURN)
        visitEnd()
    }

    lambdaBuilder.newMethod(
        JvmDeclarationOrigin.NO_ORIGIN,
        ACC_PUBLIC or ACC_FINAL or ACC_SYNTHETIC,
        "invoke",
        Type.getMethodDescriptor(
            AsmTypes.OBJECT_TYPE,
            AsmTypes.OBJECT_TYPE, // CoroutineScope
            AsmTypes.OBJECT_TYPE // Continuation
        ), null, null
    ).apply {
        visitCode()

        /*
        visitVarInsn(Opcodes.ALOAD, 1)
        val coroutineScopeInternalName = "Lkotlinx/coroutines/CoroutineScope"
        visitTypeInsn(
            Opcodes.CHECKCAST,
            coroutineScopeInternalName
        )
        */


        for (param in allParameters) {
            visitVarInsn(ALOAD, 0)
            visitFieldInsn(
                GETFIELD,
                lambdaBuilder.thisName,
                param.name.identifierOrMappedSpecialName,
                param.type.asmType().descriptor
            )
        }

        // load the second param and cast to Continuation
        visitVarInsn(ALOAD, 2)
        val continuationInternalName = state.languageVersionSettings.continuationAsmType().internalName
        visitTypeInsn(
            CHECKCAST,
            continuationInternalName
        )

        visitMethodInsn(
            INVOKEVIRTUAL,
            parentName,
            originFunction.name.identifierOrMappedSpecialName,
            Type.getMethodDescriptor(
                Type.getType(Any::class.java),
                *allParameters.drop(1).map { it.type.asmType() }.toTypedArray(),
                state.languageVersionSettings.continuationAsmType()
            ),
            false
        )
        visitInsn(ARETURN)
        visitEnd()
    }

    writeSyntheticClassMetadata(lambdaBuilder, state)

    lambdaBuilder.done()
    return lambdaBuilder.thisName
}


private fun <T> T.repeatAsList(n: Int): List<T> {
    val result = ArrayList<T>()
    repeat(n) { result.add(this) }
    return result
}