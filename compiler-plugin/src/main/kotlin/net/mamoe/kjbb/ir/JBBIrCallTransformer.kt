package net.mamoe.kjbb.ir

import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.isOverridable
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import java.util.*

val JVM_BLOCKING_BRIDGE_FQ_NAME = FqName("net.mamoe.kjbb.JvmBlockingBridge")

object KOTLINX_COROUTINES {
    private val pkg = FqName("kotlinx.coroutines")
    val RUN_BLOCKING = pkg.child(Name.identifier("runBlocking"))
    val CoroutineScope = pkg.child(Name.identifier("CoroutineScope"))
}

val IrFunction.bridgeFunctionName: Name get() = Name.identifier("${this.name}")

val ORIGIN_JVM_BLOCKING_BRIDGE: IrDeclarationOrigin get() = IrDeclarationOrigin.DEFINED

private fun IrPluginContext.referenceFunctionRunBlocking(): IrSimpleFunctionSymbol {
    return referenceFunctions(KOTLINX_COROUTINES.RUN_BLOCKING).singleOrNull()
        ?: error("kotlinx.coroutines.runBlocking not found.")
}

private fun IrPluginContext.referenceCoroutineScope(): IrClassSymbol {
    return referenceClass(KOTLINX_COROUTINES.CoroutineScope)
        ?: error("kotlinx.coroutines.CoroutineScope not found.")
}

@Suppress("ClassName")
private object Origin_JVM_BLOCKING_BRIDGE : IrDeclarationOriginImpl("JVM_BLOCKING_BRIDGE", isSynthetic = true)

fun IrPluginContext.lowerOriginFunction(originFunction: IrFunction): List<IrDeclaration>? {
    val originClass = originFunction.parentAsClass

    val bridgeFunction = buildFun {
        updateFrom(originFunction)
        isSuspend = false // keep

        origin = ORIGIN_JVM_BLOCKING_BRIDGE

        name = originFunction.bridgeFunctionName
        modality = Modality.OPEN
        returnType = originFunction.returnType

        isExternal = false // TODO: 2020/7/7 handle external
        // TODO: 2020/7/5 handle EXPECT
    }.apply fn@{
        this.copyAttributes(originFunction as IrAttributeContainer)
        this.annotations = originClass.annotations

        this.parent = originClass

        this.copyParameterDeclarationsFrom(originFunction)

        this.extensionReceiverParameter = originFunction.extensionReceiverParameter?.copyTo(this@fn)
        this.dispatchReceiverParameter = originFunction.dispatchReceiverParameter?.copyTo(this@fn)

        this.body = createIrBuilder(symbol).irBlockBody {

            // public fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T

            // given: suspend fun <T, R, ...> T.test(params): R
            // gen:           fun <T, R, ...> T.test(params): R

            val runBlockingFun = referenceFunctionRunBlocking()
            // call `kotlinx.coroutines.runBlocking<R>(CoroutineContext = ..., suspend CoroutineScope.() -> R): R`

            val suspendLambda = createSuspendLambda(
                parent = this@fn,
                lambdaType = symbols.suspendFunctionN(1) // suspend CoroutineScope.() -> R
                    .typeWith(referenceCoroutineScope().defaultType, this@fn.returnType),
                originFunction = originFunction
            ).also { +it }

            +irReturn(
                irCall(runBlockingFun).apply {
                    putTypeArgument(0, this@fn.returnType) // the R for runBlocking

                    // take default value for value argument 0

                    putValueArgument(1, irCall(suspendLambda.primaryConstructor!!).apply {
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

fun IrFunction.paramsAndReceiversAsParamsList(): List<IrValueParameter> {
    val result = mutableListOf<IrValueParameter>()
    this.dispatchReceiverParameter?.let(result::add)
    this.extensionReceiverParameter?.let(result::add)
    this.valueParameters.let(result::addAll)
    return result
}

val Name.identifierOrMappedSpecialName: String
    get() {
        return when (this.asString()) {
            "<this>" -> "\$receiver" // finally synthesized as
            else -> this.identifier
        }
    }

/**
 * Generate an anonymous object.
 *
 * - extends `suspend CoroutineScope.() -> Unit`.
 * - takes dispatch and extension receivers as param, followed by normal params, to constructor
 */
fun IrPluginContext.createSuspendLambda(
    parent: IrDeclarationParent,
    lambdaType: IrSimpleType,
    originFunction: IrFunction
): IrClass {
    @Suppress("RemoveExplicitTypeArguments") // Kotlin 1.4-M3 bug
    return buildClass {
        name = SpecialNames.NO_NAME_PROVIDED
        kind = ClassKind.CLASS
        //isInner = true
    }.apply<IrClass> clazz@{
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
                listOf(irClass.superTypes[0].getClass()!!.functions.single { it.name.identifier == "invoke" && it.isOverridable }.symbol)

            // don't removed. required by supertype.
            addValueParameter("\$scope", referenceCoroutineScope().defaultType)

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

fun IrPluginContext.createIrBuilder(
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET
) = DeclarationIrBuilder(this, symbol, startOffset, endOffset)


inline fun IrBuilderWithScope.irBlock(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    origin: IrStatementOrigin? = null,
    resultType: IrType? = null,
    isTransparent: Boolean = false,
    body: IrBlockBuilder.() -> Unit
): IrContainerExpression =
    IrBlockBuilder(
        context, scope,
        startOffset,
        endOffset,
        origin, resultType, isTransparent
    ).block(body)

inline fun IrBuilderWithScope.irBlockBody(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    body: IrBlockBodyBuilder.() -> Unit
): IrBlockBody =
    IrBlockBodyBuilder(
        context, scope,
        startOffset,
        endOffset
    ).blockBody(body)

