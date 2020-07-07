package net.mamoe.kjbb.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.isOverridable
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

val JVM_BLOCKING_BRIDGE_FQ_NAME = FqName("net.mamoe.kjbb.JvmBlockingBridge")

object KOTLINX_COROUTINES {
    private val pkg = FqName("kotlinx.coroutines")
    val RUN_BLOCKING = pkg.child(Name.identifier("runBlocking"))
    val CoroutineScope = pkg.child(Name.identifier("CoroutineScope"))
}

val IrFunction.bridgeFunctionName: Name get() = Name.identifier("${this.name}") // TODO: 2020/7/3

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
    println("lowering function ${originFunction.name}")
    val originClass = originFunction.parentAsClass

    val perquisite = mutableListOf<IrDeclaration>()

    val bridgeFunction = buildFun {
        updateFrom(originFunction)
        origin = ORIGIN_JVM_BLOCKING_BRIDGE

        name = originFunction.bridgeFunctionName
        modality = Modality.OPEN
        returnType = originFunction.returnType

        isExternal = false
        isInline = false
        isOperator = false
        // TODO: 2020/7/5 handle EXPECT
        isSuspend = false
    }.apply fn@{
        copyAttributes(originFunction as IrAttributeContainer)
        this.parent = originClass

        this.copyParameterDeclarationsFrom(originFunction)

        this.extensionReceiverParameter = originFunction.extensionReceiverParameter?.copyTo(this@fn)
        this.dispatchReceiverParameter = originFunction.dispatchReceiverParameter?.copyTo(this@fn)

        this.body = createIrBuilder(symbol).irBlockBody {

            // public fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T

            // given: suspend fun <T, R, ...> T.test(params): R
            // gen:           fun <T, R, ...> T.test(params): R

            val runBlockingFun = referenceFunctionRunBlocking()
            +irReturn(
                irBlock {
                    // call `kotlinx.coroutines.runBlocking<R>(CoroutineContext = ..., suspend CoroutineScope.() -> R): R`

                    val suspendLambda = createSuspendLambda(
                        parent = this@fn,
                        objectName = "${originClass.name}\$\$${originFunction.name}\$blocking_bridge",
                        receiverType = originClass.defaultType,
                        // probably
                        lambdaType = symbols.suspendFunctionN(1)
                            .typeWith(referenceCoroutineScope().defaultType, this@fn.returnType),
                        returnType = this@fn.returnType
                    ) {
                        val irFunction = this
                        //this.createDispatchReceiverParameter()
                        this.addValueParameter("scope", referenceCoroutineScope().defaultType)
                        this.returnType = this@fn.returnType
                        this.body = createIrBuilder(this.symbol).irBlockBody {
                            //return@irBlockBody
                            +irCall(originFunction.symbol).apply {
                                val field =
                                    irFunction.parentAsClass.fields.single { it.name.identifier == BRIDGE_CLASS_RECEIVER_NAME }
                                this.dispatchReceiver = irGetField(irGet(irFunction.dispatchReceiverParameter!!), field)

                                /*
                                // TODO: 2020/7/7 extension support
                                this@fn.extensionReceiverParameter?.let { receiver ->
                                    this.extensionReceiver = irGet(receiver)
                                }*/

                                /*
                                // TODO: 2020/7/7 type and value arguments support
                                originFunction.typeParameters.forEachIndexed { index, param ->
                                    this.putTypeArgument(index, this@fn.typeParameters[index].defaultType)
                                }
                                originFunction.valueParameters.forEachIndexed { index, param ->
                                    this.putValueArgument(index, irGet(this@fn.valueParameters[index]))
                                }*/
                            }
                        }
                    }.also { +it }

                    +irCall(runBlockingFun).apply {
                        putTypeArgument(0, this@fn.returnType) // the R for runBlocking

                        // take default value for value argument 0

                        putValueArgument(1, irCall(suspendLambda.primaryConstructor!!).apply {
                            putValueArgument(0, irGet(this@fn.dispatchReceiverParameter!!))
                        })
                    }
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

    return perquisite + listOf(bridgeFunction)
}

private val BRIDGE_CLASS_RECEIVER_NAME = "\$p"

/**
 * Generate an anonymous object extending `suspend CoroutineScope.() -> Unit`
 */
fun IrPluginContext.createSuspendLambda(
    parent: IrDeclarationParent,
    objectName: String,
    receiverType: IrType,
    lambdaType: IrType,
    returnType: IrType,
    body: IrSimpleFunction.() -> Unit
): IrClass {
    /*
    val s: suspend CoroutineScope.() -> R = {

    }
     */

    return buildClass {
        name = SpecialNames.NO_NAME_PROVIDED
        kind = ClassKind.CLASS
        visibility = Visibilities.PUBLIC
        //isInner = true
    }.apply clazz@{
        this.parent = parent
        superTypes = listOf(lambdaType)


        val receiverProp = this.addField {
            this.name = Name.identifier(BRIDGE_CLASS_RECEIVER_NAME)
            this.type = receiverType//.remapTypeParameters(parent as IrTypeParametersContainer, this@clazz)
        }

        createImplicitParameterDeclarationWithWrappedDescriptor()
        addConstructor {
            isPrimary = true
        }.apply {
            val receiverParam = this.addValueParameter {
                this.name = Name.identifier("p")
                this.type = receiverProp.type
            }
            this.body = createIrBuilder(this.symbol).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                +irSetField(irGet(this@clazz.thisReceiver!!), receiverProp, irGet(receiverParam))
            }
        }
        val irClass = this
        val invoke = addFunction("invoke", returnType, isSuspend = true).apply {
            this.overriddenSymbols =
                listOf(irClass.superTypes[0].getClass()!!.functions.single { it.name.identifier == "invoke" && it.isOverridable }.symbol)
            body()
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

