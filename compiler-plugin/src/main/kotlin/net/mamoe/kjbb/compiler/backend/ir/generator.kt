package net.mamoe.kjbb.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.backend.jvm.codegen.fileParent
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import java.util.*
import org.jetbrains.kotlin.ir.util.isInterface as isInterfaceKotlin


internal object IntrinsicRuntimeFunctions {
    private val pkg = FqName("net.mamoe.kjbb.internal")
    val RUN_SUSPEND = pkg.child(Name.identifier("\$runSuspend\$"))
}

internal val IrFunction.bridgeFunctionName: Name get() = Name.identifier("${this.name}")

internal val ORIGIN_JVM_BLOCKING_BRIDGE: IrDeclarationOrigin? get() = IrDeclarationOrigin.DEFINED

private fun IrPluginContext.referenceFunctionRunBlocking(): IrSimpleFunctionSymbol {
    return referenceFunctions(IntrinsicRuntimeFunctions.RUN_SUSPEND).singleOrNull()
        ?: error("Internal error: Function ${IntrinsicRuntimeFunctions.RUN_SUSPEND} not found.")
}

private fun IrPluginContext.referenceJvmDefault(): IrClassSymbol {
    val s = FqName("kotlin.jvm").child(Name.identifier("JvmDefault"))
    return referenceClass(s)
        ?: error("Internal error: Function ${s} not found.")
}

@Suppress("ClassName")
private object Origin_JVM_BLOCKING_BRIDGE : IrDeclarationOriginImpl("JVM_BLOCKING_BRIDGE", isSynthetic = true)

internal fun IrFunction.isExplicitOrImplicitStatic(): Boolean {
    return this.isStatic // || this.hasAnnotation(JVM_STATIC_FQ_NAME)
}

internal fun IrPluginContext.createGeneratedBlockingBridgeConstructorCall(
    symbol: IrSymbol,
): IrConstructorCall {
    return createIrBuilder(symbol).irAnnotationConstructor(referenceClass(GENERATED_BLOCKING_BRIDGE_FQ_NAME)!!)
}

internal fun IrBuilderWithScope.irAnnotationConstructor(
    clazz: IrClassSymbol,
): IrConstructorCall {
    return run {
        irCall(clazz.constructors.first())
    }.run {
        irConstructorCall(this, this.symbol)
    }
}


internal val IrDeclarationContainer.functionsSequence: Sequence<IrSimpleFunction>
    get() = declarations.asSequence().filterIsInstance<IrSimpleFunction>()

internal fun IrDeclarationContainer.hasDuplicateBridgeFunction(originFunction: IrFunction): Boolean {
    val params = originFunction.allParameters
    val typePrams = originFunction.allTypeParameters
    return this.functionsSequence
        .filter { !it.isSuspend }
        .filter { it.name == originFunction.name }
        .filter { it.allParameters == params }
        .filter { it.allTypeParameters == typePrams }
        .any()
}

internal val IrDeclaration.parentFileOrClass: IrDeclarationContainer get() = this.parentClassOrNull ?: fileParent

internal fun IrFunction.hasDuplicateBridgeFunction(): Boolean = parentFileOrClass.hasDuplicateBridgeFunction(this)

internal fun IrType.isClassType(fqName: FqNameUnsafe, hasQuestionMark: Boolean? = null): Boolean {
    if (this !is IrSimpleType) return false
    if (hasQuestionMark != null && this.hasQuestionMark != hasQuestionMark) return false
    return classifier.isClassWithFqName(fqName)
}

internal val IrDeclarationContainer.isInterface: Boolean
    get() = (this as? IrClass)?.isInterfaceKotlin == true

internal val IrDeclarationContainer.isAbstract: Boolean
    get() = (this as? IrClass)?.modality == Modality.ABSTRACT

internal val IrDeclarationContainer.isOpen: Boolean
    get() = (this as? IrClass)?.modality == Modality.OPEN

internal val IrDeclarationContainer.isPrivate: Boolean
    get() = (this as? IrDeclarationWithVisibility)?.visibility == Visibilities.PRIVATE

internal val IrClass.isInsidePrivateClass: Boolean
    get() =
        this.parents.any { (it as? IrClass)?.isPrivate == true }

internal fun IrDeclarationContainer.computeModalityForBridgeFunction(): Modality {
    if (isInterface) return Modality.OPEN
    return when {
        isOpen || isAbstract -> Modality.OPEN
        else -> Modality.FINAL
    }
}

internal fun IrFunction.computeVisibilityForBridgeFunction(): Visibility {
    if (parentFileOrClass.isInterface) return Visibilities.PUBLIC
    return this.visibility
}

fun IrPluginContext.generateJvmBlockingBridges(originFunction: IrFunction): List<IrDeclaration> {
    val containingFileOrClass = originFunction.parentFileOrClass

    val bridgeFunction = buildFun {
        startOffset = originFunction.startOffset
        endOffset = originFunction.endOffset

        origin = ORIGIN_JVM_BLOCKING_BRIDGE ?: originFunction.origin

        name = originFunction.bridgeFunctionName
        returnType = originFunction.returnType

        modality = containingFileOrClass.computeModalityForBridgeFunction()
        visibility = originFunction.computeVisibilityForBridgeFunction() // avoid suppressing

        isSuspend = false
        isExternal = false
        isExpect = false
    }.apply fn@{
        this.copyAttributes(originFunction as IrAttributeContainer)
        this.copyParameterDeclarationsFrom(originFunction)

        this.annotations = originFunction.annotations
            .filterNot { it.type.isClassType(JVM_BLOCKING_BRIDGE_FQ_NAME.toUnsafe()) }
            .plus(createGeneratedBlockingBridgeConstructorCall(symbol))

        if (containingFileOrClass.isInterface) {
            this.annotations += createIrBuilder(symbol).irAnnotationConstructor(referenceJvmDefault())
        }

        this.parent = containingFileOrClass

        this.extensionReceiverParameter = originFunction.extensionReceiverParameter?.copyTo(this@fn)
        this.dispatchReceiverParameter =
            if (originFunction.isExplicitOrImplicitStatic()) null
            else originFunction.dispatchReceiverParameter?.copyTo(this@fn)

        this.body = createIrBuilder(symbol).irBlockBody {

            // public fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T

            // given: suspend fun <T, R, ...> T.test(params): R
            // gen:           fun <T, R, ...> T.test(params): R

            val runBlockingFun = referenceFunctionRunBlocking()
            // call `kotlinx.coroutines.runBlocking<R>(CoroutineContext = ..., suspend CoroutineScope.() -> R): R`

            val suspendLambda = createSuspendLambdaWithCoroutineScope(
                parent = this@fn,
                lambdaType = symbols.suspendFunctionN(0).typeWith(this@fn.returnType) // suspend () -> R
                ,
                originFunction = originFunction
            ).also { +it }

            +irReturn(
                irCall(runBlockingFun).apply {
                    // putTypeArgument(0, this@fn.returnType) // the R for runBlocking

                    // take default value for value argument 0

                    putValueArgument(0, irCall(suspendLambda.primaryConstructor!!).apply {
                        for ((index, parameter) in this@fn.paramsAndReceiversAsParamsList().withIndex()) {
                            putValueArgument(index, irGet(parameter))
                        }
                    })
                }
            )
            /*

            +irReturn(irCall(originFunction).apply {
                for (param in valueParameters) {
                    addArguments(valueParameters.associateWith { irGet(it) }.mapKeys { it.key.descriptor })
                }
            })

            */
        }
    }

    return listOf(bridgeFunction)
}

internal fun IrFunction.paramsAndReceiversAsParamsList(): List<IrValueParameter> {
    val result = mutableListOf<IrValueParameter>()
    if (!this.isExplicitOrImplicitStatic()) {
        this.dispatchReceiverParameter?.let(result::add)
    }
    this.extensionReceiverParameter?.let(result::add)
    this.valueParameters.let(result::addAll)
    return result
}

internal val Name.identifierOrMappedSpecialName: String
    get() {
        return when (this.asString()) {
            "<this>" -> "\$receiver" // finally synthesized as
            else -> this.identifier
        }
    }

/**
 * Generates an anonymous object.
 *
 * - extends `suspend () -> Unit`.
 * - takes dispatch and extension receivers as param, followed by normal value params, to the constructor of this object
 */
internal fun IrPluginContext.createSuspendLambdaWithCoroutineScope(
    parent: IrDeclarationParent,
    lambdaType: IrSimpleType,
    originFunction: IrFunction,
): IrClass {
    return buildClass {
        name = SpecialNames.NO_NAME_PROVIDED
        kind = ClassKind.CLASS
        //isInner = true
    }.apply clazz@{
        this.parent = parent
        superTypes = listOf(lambdaType)

        val fields = originFunction.paramsAndReceiversAsParamsList().map {
            addField(it.name.identifierOrMappedSpecialName.synthesizedName, it.type)
        }

        createImplicitParameterDeclarationWithWrappedDescriptor()

        addConstructor {
            isPrimary = true
        }.apply constructor@{
            val newParams = fields.associateWith { irField ->
                this@constructor.addValueParameter {
                    name = irField.name
                    type = irField.type
                }
            }

            this@constructor.body = createIrBuilder(symbol).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())

                for ((irField, irValueParam) in newParams) {
                    +irSetField(irGet(this@clazz.thisReceiver!!), irField, irGet(irValueParam))
                }
            }
        }

        val irClass = this

        addFunction("invoke", lambdaType.arguments.last().typeOrNull!!, isSuspend = true).apply functionInvoke@{
            this.overriddenSymbols =
                listOf(irClass.superTypes[0].getClass()!!.functionsSequence.single { it.name.identifier == "invoke" && it.isOverridable }.symbol)

            //this.createDispatchReceiverParameter()
            this.body = createIrBuilder(symbol).run {
                // don't use expr body, coroutine codegen can't generate for it.
                irBlockBody {
                    +irCall(originFunction).apply call@{
                        // set arguments

                        val arguments = fields.mapTo(LinkedList()) { it } // preserve order

                        fun IrField.irGetField(): IrGetFieldImpl {
                            return irGetField(irGet(this@functionInvoke.dispatchReceiverParameter!!), this)
                        }

                        if (originFunction.dispatchReceiverParameter != null) {
                            this@call.dispatchReceiver = arguments.pop().irGetField()
                        }
                        if (originFunction.extensionReceiverParameter != null) {
                            this@call.extensionReceiver = arguments.pop().irGetField()
                        }

                        // this@call.putValueArgument(0, irGet(scopeParam))
                        for ((index, irField) in arguments.withIndex()) {
                            this@call.putValueArgument(index, irField.irGetField())
                        }
                    }
                }
            }
        }
    }
}

internal fun IrPluginContext.createIrBuilder(
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET,
) = DeclarationIrBuilder(this, symbol, startOffset, endOffset)


@Suppress("unused")
internal fun IrBuilderWithScope.irBlock(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    origin: IrStatementOrigin? = null,
    resultType: IrType? = null,
    isTransparent: Boolean = false,
    body: IrBlockBuilder.() -> Unit,
): IrContainerExpression =
    IrBlockBuilder(
        context, scope,
        startOffset,
        endOffset,
        origin, resultType, isTransparent
    ).block(body)

internal fun IrBuilderWithScope.irBlockBody(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    body: IrBlockBodyBuilder.() -> Unit,
): IrBlockBody =
    IrBlockBodyBuilder(
        context, scope,
        startOffset,
        endOffset
    ).blockBody(body)

