package net.mamoe.kjbb.ir

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class JvmBlockingBridgeLoweringPass(
    private val context: IrPluginContext
) : ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        val transformer = object : IrElementTransformerVoid() {
            val newDeclarations = mutableListOf<IrDeclaration>()

            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)) {
                    println("lowering ${declaration.parentAsClass.name}.${declaration.name}")
                    check(declaration.canGenerateJvmBlockingBridge()) {
                        // TODO: 2020/7/8 DIAGNOSTICS
                        "@JvmBlockingBridge is not applicable to function '${declaration.name}'"
                    }
                    if (declaration.isFakeOverride || declaration.overriddenSymbols
                            .any { it is IrSimpleFunction && it.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME) }
                    ) {
                        println("ignored ${declaration.parentAsClass.name}.${declaration.name}")
                        return declaration
                    }
                    check(!declaration.hasDuplicateBridgeFunction()) {
                        // TODO: 2020/7/8 DIAGNOSTICS FROM PLATFORM_DECLARE_CLASH
                        "PLATFORM_DECLARE_CLASH: function '${declaration.name}'"
                    }
                    newDeclarations.addAll(context.generateJvmBlockingBridges(declaration))
                }

                return declaration
            }
        }
        irClass.transformChildrenVoid(transformer)
        transformer.newDeclarations.forEach(irClass::addMember)
    }
}