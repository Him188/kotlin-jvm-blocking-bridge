package net.mamoe.kjbb.jvm

import net.mamoe.kjbb.ir.GENERATED_BLOCKING_BRIDGE_FQ_NAME
import net.mamoe.kjbb.ir.JVM_BLOCKING_BRIDGE_FQ_NAME
import net.mamoe.kjbb.ir.identifierOrMappedSpecialName
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import kotlin.reflect.KClass

interface BridgeCodegenExtensions {
    val typeMapper: KotlinTypeMapper

    fun KotlinType.asmType(): Type = this.asmType(typeMapper)

    fun FunctionDescriptor.owner(superCall: Boolean): Type = typeMapper.mapToCallableMethod(this, superCall).owner
}

val GENERATED_BLOCKING_BRIDGE_ASM_TYPE = GENERATED_BLOCKING_BRIDGE_FQ_NAME.topLevelClassAsmType()
val JVM_BLOCKING_BRIDGE_ASM_TYPE = JVM_BLOCKING_BRIDGE_FQ_NAME.topLevelClassAsmType()


class BridgeCodegen(
    private val codegen: ImplementationBodyCodegen,
    private val generationState: GenerationState = codegen.state
) : BridgeCodegenExtensions {
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
        val methodSignature = originFunction.computeJvmDescriptor(withName = true)
        val returnType = originFunction.returnType?.asmType()
            ?: Nothing::class.asmType

        val mv = v.newMethod(
            OtherOrigin(originFunction),
            ACC_PUBLIC, // TODO: 2020/7/16 or FINAL?
            methodName,
            extensionReceiverAndValueParameters().computeJvmDescriptorForMethod(
                typeMapper,
                returnTypeDescriptor = returnType.descriptor
            ),
            methodSignature,
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

        FunctionCodegen.generateParameterAnnotations(
            originFunction,
            mv,
            typeMapper.mapSignatureWithCustomParameters(
                originFunction,
                codegen.context.contextKind,
                originFunction.valueParameters,
                true
            ),
            originFunction.valueParameters,
            codegen,
            generationState
        )

        for ((index, param) in originFunction.extensionReceiverAndValueParameters().withIndex()) {
            for (annotation in param.annotations) {
                mv.visitParameterAnnotation(index, annotation.type.asmType().descriptor, true)
            }
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

        mv.genAnnotation(GENERATED_BLOCKING_BRIDGE_ASM_TYPE.descriptor, true)

        if (!generationState.classBuilderMode.generateBodies) {
            FunctionCodegen.endVisit(mv, methodName, clazz.findPsi())
            return
        }

        // body

        val ownerType = clazz.defaultType.asmType()

        val iv = InstructionAdapter(mv)

        val lambdaClassDescriptor = generateLambdaForRunBlocking(
            originFunction, codegen.state,
            originFunction.findPsi()!!,
            codegen.v.thisName,
            codegen.context.contextKind
        ).let { Type.getObjectType(it) }

        //mv.visitParameter("\$\$this", 1)
        //mv.visitParameter("arg1", 2)

        mv.visitCode()
        //  AsmUtil.

        with(iv) {

            val stack = FrameMap()

            aconst(null) // coroutineContext

            // call lambdaConstructor
            anew(lambdaClassDescriptor)// mv.visitTypeInsn(NEW, lambdaClassDescriptor.internalName)
            dup()

            val thisIndex = stack.enterTemp(AsmTypes.OBJECT_TYPE)
            load(thisIndex, ownerType) // dispatch

            for (parameter in originFunction.extensionReceiverAndValueParameters()) {
                val asmType = parameter.type.asmType()
                load(stack.enterTemp(asmType), asmType)
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

            genReturn(returnType)
        }

        FunctionCodegen.endVisit(mv, methodName, clazz.findPsi())
    }
}

internal val OBJECT_ASM_TYPE = Any::class.asmType
internal val VOID_ASM_TYPE = Any::class.asmType

internal fun InstructionAdapter.genReturn(type: Type) {
    StackValue.coerce(OBJECT_ASM_TYPE, type, this)
    areturn(type)
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

/**
 * @see Type.getDescriptor
 */
internal val KClass<*>.asmType: Type
    get() = Type.getType(this.java)

internal fun FunctionDescriptor.extensionReceiverAndValueParameters(): List<ParameterDescriptor> {
    return extensionReceiverParameter.followedBy(valueParameters)
}

internal operator fun <T> T.plus(list: Collection<T>): List<T> {
    val new = ArrayList<T>(list.size + 1)
    new.add(this)
    new.addAll(list)
    return new
}

internal fun <T> T?.followedBy(list: Collection<T>): List<T> {
    if (this == null) return list.toList()
    val new = ArrayList<T>(list.size + 1)
    new.add(this)
    new.addAll(list)
    return new
}

internal fun List<ParameterDescriptor>.computeJvmDescriptorForMethod(
    typeMapper: KotlinTypeMapper,
    returnTypeDescriptor: String
): String {
    return Type.getMethodDescriptor(
        Type.getType(returnTypeDescriptor),
        *this.map { it.type.asmType(typeMapper) }.toTypedArray()
    )
}

internal fun List<ParameterDescriptor>.computeJvmDescriptorForMethod(
    typeMapper: KotlinTypeMapper,
    vararg additionalParameterTypes: Type,
    returnTypeDescriptor: String
): String {
    return Type.getMethodDescriptor(
        Type.getType(returnTypeDescriptor),
        *this.map { it.type.asmType(typeMapper) }.toTypedArray(),
        *additionalParameterTypes
    )
}

internal const val DISPATCH_RECEIVER_VAR_NAME = "p\$"

private fun BridgeCodegenExtensions.generateLambdaForRunBlocking(
    originFunction: FunctionDescriptor,
    state: GenerationState,
    originElement: PsiElement,
    parentName: String,
    contextKind: OwnerKind
): String {
    val methodOwnerType = originFunction.owner(false)
    val isStatic = AsmUtil.isStaticMethod(contextKind, originFunction)

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


    fun ClassBuilder.genNewField(parameter: ParameterDescriptor, name: String? = null) {
        newField(
            JvmDeclarationOrigin.NO_ORIGIN,
            ACC_PRIVATE or ACC_FINAL,
            name ?: parameter.synthesizedNameString(),
            parameter.type.asmType().descriptor, null, null
        )
    }
    originFunction.dispatchReceiverParameter?.let {
        lambdaBuilder.genNewField(it, DISPATCH_RECEIVER_VAR_NAME)
    }
    for (parameter in originFunction.extensionReceiverAndValueParameters()) {
        lambdaBuilder.genNewField(parameter)
    }


    /*
    val flags =
        baseMethodFlags or
                (if (isStatic) Opcodes.ACC_STATIC else 0) or
                ACC_FINAL or
                (if (allParameters.lastOrNull()?.varargElementType != null) Opcodes.ACC_VARARGS else 0)
*/

    lambdaBuilder.newMethod(
        JvmDeclarationOrigin.NO_ORIGIN,
        AsmUtil.NO_FLAG_PACKAGE_PRIVATE or ACC_SYNTHETIC,
        "<init>",
        originFunction.explicitParameters.computeJvmDescriptorForMethod(typeMapper, "V"), null, null
    ).applyWithInstructionAdapter {
        visitCode()

        val stack = FrameMap()

        val methodBegin = Label()
        visitLabel(methodBegin)

        val thisIndex = stack.enterTemp(AsmTypes.OBJECT_TYPE)

        // set fields

        fun InstructionAdapter.genPutField(parameter: ParameterDescriptor, name: String? = null) {
            val asmType = parameter.type.asmType()

            load(thisIndex, methodOwnerType)
            load(stack.enterTemp(asmType), asmType)

            visitFieldInsn(
                PUTFIELD,
                lambdaBuilder.thisName,
                name ?: parameter.synthesizedNameString(),
                asmType.descriptor
            )
        }

        originFunction.dispatchReceiverParameter?.let { param ->
            genPutField(param, DISPATCH_RECEIVER_VAR_NAME)
        }

        for (valueParameter in originFunction.extensionReceiverAndValueParameters()) {
            genPutField(valueParameter)
        }

        invokeSuperLambdaConstructor(2) // super(2)

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
    ).applyWithInstructionAdapter {
        visitCode()

        val stack = FrameMap()
        val thisIndex = stack.enterTemp(AsmTypes.OBJECT_TYPE)


        if (!isStatic) { // load this for the dispatch param at the end
            load(thisIndex, methodOwnerType)
        }

        fun InstructionAdapter.genGetField(parameter: ParameterDescriptor, name: String? = null) {
            val asmType = parameter.type.asmType()

            load(thisIndex, methodOwnerType)
            // load(stack.enterTemp(asmType), asmType)

            visitFieldInsn(
                GETFIELD,
                lambdaBuilder.thisName,
                name ?: parameter.synthesizedNameString(),
                asmType.descriptor
            )
        }

        originFunction.dispatchReceiverParameter?.let { genGetField(it, DISPATCH_RECEIVER_VAR_NAME) }
        for (parameter in originFunction.extensionReceiverAndValueParameters()) {
            genGetField(parameter)
        }


        // load the second param and cast to Continuation
        visitVarInsn(ALOAD, 2)
        val continuationInternalName = state.languageVersionSettings.continuationAsmType().internalName
        visitTypeInsn(
            CHECKCAST,
            continuationInternalName
        )


        // call origin function
        visitMethodInsn(
            if (isStatic) INVOKESTATIC else INVOKEVIRTUAL, // OR INVOKESTATIC
            parentName,
            originFunction.name.identifier,
            Type.getMethodDescriptor(
                AsmTypes.OBJECT_TYPE,
                *originFunction.extensionReceiverAndValueParameters().map { it.type.asmType() }.toTypedArray(),
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

internal fun ParameterDescriptor.synthesizedNameString(): String =
    this.name.identifierOrMappedSpecialName.synthesizedString

internal fun ParameterDescriptor.synthesizedName(): Name = this.name.identifierOrMappedSpecialName.synthesizedName

internal fun MethodVisitor.invokeSuperLambdaConstructor(arity: Int) {
    loadThis()
    visitInsn(
        when (arity) {
            0 -> ICONST_0
            1 -> ICONST_1
            2 -> ICONST_2
            3 -> ICONST_3
            4 -> ICONST_4
            5 -> ICONST_5
            else -> error("unsupported arity: $arity")
        }
    ) // Function2
    visitMethodInsn(
        INVOKESPECIAL,
        AsmTypes.LAMBDA.internalName,
        "<init>",
        Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE),
        false
    )
}

internal fun <T : MethodVisitor> T.applyWithInstructionAdapter(block: InstructionAdapter.() -> Unit): T {
    InstructionAdapter(this).apply(block)
    return this
}

internal fun MethodVisitor.loadThis() = visitVarInsn(ALOAD, 0)

private fun <T> T.repeatAsList(n: Int): List<T> {
    val result = ArrayList<T>()
    repeat(n) { result.add(this) }
    return result
}