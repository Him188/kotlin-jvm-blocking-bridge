package net.mamoe.kjbb.compiler.backend.jvm

import com.intellij.psi.PsiElement
import net.mamoe.kjbb.compiler.backend.ir.GENERATED_BLOCKING_BRIDGE_ASM_TYPE
import net.mamoe.kjbb.compiler.backend.ir.JVM_BLOCKING_BRIDGE_ASM_TYPE
import net.mamoe.kjbb.compiler.backend.ir.JVM_BLOCKING_BRIDGE_FQ_NAME
import net.mamoe.kjbb.compiler.backend.ir.identifierOrMappedSpecialName
import net.mamoe.kjbb.compiler.context.CompilerContext
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.coroutines.continuationAsmType
import org.jetbrains.kotlin.codegen.inline.NUMBERED_FUNCTION_PREFIX
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotatedImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.createFunctionType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import kotlin.reflect.KClass

internal interface BridgeCodegenExtensions {
    val typeMapper: KotlinTypeMapper

    fun KotlinType.asmType(): Type = this.asmType(typeMapper)

}

class BridgeCodegen(
    private val codegen: ImplementationBodyCodegen,
    compilerContext: CompilerContext = CompilerContext.INSTANCE,
) : BridgeCodegenExtensions, CompilerContext by compilerContext {

    private val generationState: GenerationState get() = codegen.state
    private inline val v get() = codegen.v
    private inline val clazz get() = codegen.descriptor
    override val typeMapper: KotlinTypeMapper get() = codegen.typeMapper

    private fun report(diagnostic: Diagnostic) {
        codegen.state.bindingTrace.report(diagnostic)
    }

    fun generate() {
        val members = clazz.unsubstitutedMemberScope
        val names = members.getFunctionNames()
        val functions =
            names.flatMap { members.getContributedFunctions(it, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS) }.toSet()

        for (function in functions) {
            if (function.annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)) {
                val capability = function.analyzeCapabilityForGeneratingBridges()
                if (!capability.diagnosticPassed) capability.createDiagnostic()?.let(::report)
                if (capability.shouldGenerate) function.generateBridge()
            }
        }
    }

    private fun SimpleFunctionDescriptor.generateBridge() {

        val originFunction = this

        val methodName = originFunction.jvmName ?: originFunction.name.asString()
        val returnType = originFunction.returnType?.asmType()
            ?: Nothing::class.asmType

        val methodOrigin = JvmDeclarationOrigin(
            originKind = JvmDeclarationOriginKind.BRIDGE,
            descriptor = originFunction,
            parametersForJvmOverload = extensionReceiverAndValueParameters().map {
                DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtParameter
            }
        )
        //val methodSignature = originFunction.computeJvmDescriptor(withName = true)
        val staticFlag = when {
            originFunction.isJvmStatic() -> ACC_STATIC
            else -> 0
        }

        val isStatic = staticFlag != 0

        val mv = v.newMethod(
            methodOrigin,
            originFunction.bridgesVisibility() or ACC_PUBLIC or staticFlag,
            methodName,
            extensionReceiverAndValueParameters().computeJvmDescriptorForMethod(
                typeMapper,
                returnTypeDescriptor = returnType.descriptor
            ),
            null,
            null
        ) // TODO: 2020/7/12 exceptions?

        if (codegen.context.contextKind != OwnerKind.ERASED_INLINE_CLASS && clazz.isInline) {
            FunctionCodegen.generateMethodInsideInlineClassWrapper(
                methodOrigin, this, clazz, mv, typeMapper
            )
            return
        }


        fun MethodVisitor.genAnnotation(
            descriptor: String,
            visible: Boolean,
            block: AnnotationVisitor.() -> Unit = {},
        ) {
            visitAnnotation(descriptor, visible)?.apply(block)?.visitEnd()
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
            .let { annotations ->
                AnnotationCodegen.forMethod(mv, codegen, generationState)
                    .genAnnotations(
                        AnnotatedImpl(Annotations.create(annotations)),
                        returnType,
                        createFunctionType(
                            builtIns,
                            suspendFunction = false,
                            shouldUseVarargType = true
                        )
                    )
            }

        mv.genAnnotation(GENERATED_BLOCKING_BRIDGE_ASM_TYPE.descriptor, true)

        if (!generationState.classBuilderMode.generateBodies) {
            FunctionCodegen.endVisit(mv, methodName, methodOrigin.element)
            return
        }

        // body

        val ownerType = clazz.defaultType.asmType()

        val iv = InstructionAdapter(mv)

        val lambdaClassDescriptor = generateLambdaForRunBlocking(
            originFunction, codegen.state,
            originFunction.findPsi()!!,
            codegen.v.thisName,
            codegen.context.contextKind,
            ownerType,
            if (isStatic) null else clazz.thisAsReceiverParameter
        ).let { Type.getObjectType(it) }

        //mv.visitParameter("\$\$this", 1)
        //mv.visitParameter("arg1", 2)

        mv.visitCode()
        //  AsmUtil.

        with(iv) {

            val stack = FrameMap()

            // call lambdaConstructor
            anew(lambdaClassDescriptor)
            dup()

            if (!isStatic) {
                val thisIndex = stack.enterTemp(AsmTypes.OBJECT_TYPE)
                load(thisIndex, ownerType) // dispatch
            }

            for (parameter in originFunction.extensionReceiverAndValueParameters()) {
                val asmType = parameter.type.asmType()
                load(stack.enterTemp(asmType), asmType)
            }

            invokespecial(
                lambdaClassDescriptor.internalName,
                "<init>",
                originFunction.allRequiredParameters(clazz.thisAsReceiverParameter)
                    .computeJvmDescriptorForMethod(typeMapper, "V"),
                false
            )

            // public fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T {
            invokestatic(
                "net/mamoe/kjbb/internal/RunBlockingKt",
                "\$runSuspend\$",
                "(Lkotlin/jvm/functions/Function1;)Ljava/lang/Object;",
                false
            )
            //cast(Type.getType(Any::class.java), originFunction.returnTypeOrNothing.asmType())
            //

            genReturn(returnType)
        }

        FunctionCodegen.endVisit(mv, methodName, methodOrigin.element)
    }
}

internal val OBJECT_ASM_TYPE = Any::class.asmType

internal fun InstructionAdapter.genReturn(type: Type) {
    StackValue.coerce(OBJECT_ASM_TYPE, type, this)
    areturn(type)
}

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

internal fun FunctionDescriptor.allRequiredParameters(dispatchReceiver: ReceiverParameterDescriptor?): List<ParameterDescriptor> {
    return if (!isJvmStatic()) dispatchReceiver!!.followedBy(extensionReceiverParameter.followedBy(valueParameters)) else extensionReceiverParameter.followedBy(
        valueParameters
    )
}

internal fun CallableDescriptor.isJvmStatic(): Boolean =
    isJvmStaticIn { true }

private fun CallableDescriptor.isJvmStaticIn(predicate: (DeclarationDescriptor) -> Boolean): Boolean =
    when (this) {
        is PropertyAccessorDescriptor -> {
            val propertyDescriptor = correspondingProperty
            predicate(propertyDescriptor.containingDeclaration) &&
                    (hasJvmStaticAnnotation() || propertyDescriptor.hasJvmStaticAnnotation())
        }
        else -> predicate(containingDeclaration) && hasJvmStaticAnnotation()
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
    returnTypeDescriptor: String,
): String {
    return Type.getMethodDescriptor(
        Type.getType(returnTypeDescriptor),
        *this.map { it.type.asmType(typeMapper) }.toTypedArray()
    )
}

internal const val DISPATCH_RECEIVER_VAR_NAME = "p\$"

private fun BridgeCodegenExtensions.generateLambdaForRunBlocking(
    originFunction: FunctionDescriptor,
    state: GenerationState,
    originElement: PsiElement,
    parentName: String,
    contextKind: OwnerKind,
    methodOwnerType: Type,
    dispatchReceiverParameterDescriptor: ReceiverParameterDescriptor?,
): String {
    val isStatic = AsmUtil.isStaticMethod(contextKind, originFunction)

    val internalName = "$parentName$$${originFunction.name}\$blocking_bridge"
    val lambdaBuilder = state.factory.newVisitor(
        OtherOrigin(originFunction),
        Type.getObjectType(internalName),
        originElement.containingFile
    )

    val arity = 1

    lambdaBuilder.defineClass(
        originElement, state.classFileVersion,
        ACC_FINAL or ACC_SUPER or ACC_SYNTHETIC,
        internalName, null,
        AsmTypes.LAMBDA.internalName,
        arrayOf(NUMBERED_FUNCTION_PREFIX + arity) // Function2<in P1, out R>
    )


    fun ClassBuilder.genNewField(parameter: ParameterDescriptor, name: String? = null) {
        newField(
            JvmDeclarationOrigin.NO_ORIGIN,
            ACC_PRIVATE or ACC_FINAL,
            name ?: parameter.synthesizedNameString(),
            parameter.type.asmType().descriptor, null, null
        )
    }
    dispatchReceiverParameterDescriptor?.let {
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
        originFunction.allRequiredParameters(dispatchReceiverParameterDescriptor)
            .computeJvmDescriptorForMethod(typeMapper, "V"), null, null
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

        dispatchReceiverParameterDescriptor?.let { param ->
            genPutField(param, DISPATCH_RECEIVER_VAR_NAME)
        }

        for (valueParameter in originFunction.extensionReceiverAndValueParameters()) {
            genPutField(valueParameter)
        }

        invokeSuperLambdaConstructor(arity) // super(1)

        visitInsn(RETURN)
        visitEnd()
    }

    lambdaBuilder.newMethod(
        JvmDeclarationOrigin.NO_ORIGIN,
        ACC_PUBLIC or ACC_SYNTHETIC or ACC_FINAL,
        "invoke",
        Type.getMethodDescriptor(
            AsmTypes.OBJECT_TYPE,
            // AsmTypes.OBJECT_TYPE, // CoroutineScope
            AsmTypes.OBJECT_TYPE // Continuation
        ), null, null
    ).applyWithInstructionAdapter {
        visitCode()

        val stack = FrameMap()
        val thisIndex = stack.enterTemp(AsmTypes.OBJECT_TYPE)

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

        dispatchReceiverParameterDescriptor?.let { genGetField(it, DISPATCH_RECEIVER_VAR_NAME) }
        for (parameter in originFunction.extensionReceiverAndValueParameters()) {
            genGetField(parameter)
        }


        // load last function parameter and cast to Continuation
        visitVarInsn(ALOAD, arity) // 0 is `this`, 1 is 1st param, arity is the last
        val continuationInternalName = state.languageVersionSettings.continuationAsmType().internalName
        visitTypeInsn(
            CHECKCAST,
            continuationInternalName
        )


        // call origin function
        visitMethodInsn(
            when {
                isStatic -> INVOKESTATIC
                originFunction.containingClass?.isInterface() == true -> INVOKEINTERFACE
                else -> INVOKEVIRTUAL
            },
            parentName,
            originFunction.jvmNameOrName.identifier,
            Type.getMethodDescriptor(
                AsmTypes.OBJECT_TYPE,
                *originFunction.extensionReceiverAndValueParameters().map { it.type.asmType() }.toTypedArray(),
                state.languageVersionSettings.continuationAsmType()
            ),
            false)
        visitInsn(ARETURN)
        visitEnd()
    }

    writeSyntheticClassMetadata(lambdaBuilder, state)

    lambdaBuilder.done()
    return lambdaBuilder.thisName
}

private fun FunctionDescriptor.bridgesVisibility(): Int {
    val containingClass = containingClass ?: return ACC_FINAL
    return when (containingClass.kind) {
        ENUM_ENTRY,
        ANNOTATION_CLASS,
        ENUM_CLASS,
        OBJECT,
        CLASS,
        -> ACC_FINAL
        INTERFACE -> 0
    }
}

internal fun ParameterDescriptor.synthesizedNameString(): String =
    this.name.identifierOrMappedSpecialName.synthesizedString

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
    ) // FunctionN
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

