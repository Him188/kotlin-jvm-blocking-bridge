package net.mamoe.kjbb.ir

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isClass
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.visitors.*


private class JBBIrCallTransformer(
    val context: IrPluginContext
) : IrElementTransformerVoid(), ClassLoweringPass {
    val newDeclarations = mutableListOf<IrDeclaration>()

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val simpleFunction = declaration as? IrSimpleFunction ?: return declaration

        if (simpleFunction.overriddenSymbols
                .map { it.owner }
                .plus(simpleFunction)
                .reversed()
                .any { it.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME) }
        ) {
            newDeclarations.addAll(context.lowerOriginFunction(declaration).orEmpty())
        }

        return declaration
    }

    override fun lower(irClass: IrClass) {
        if (!(irClass.isClass || irClass.isObject)) return

        irClass.transformChildrenVoid(this)

        newDeclarations.forEach(irClass::addMember)
    }
}


class JvmBlockingBridgeIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        for (file in moduleFragment.files) {
            JBBIrCallTransformer(pluginContext).runOnFileInOrder(file)
        }
    }
}

fun ClassLoweringPass.runOnFileInOrder(irFile: IrFile) {
    irFile.acceptChildrenVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            lower(declaration) // lower bridge before lowering suspend
            //declaration.acceptChildrenVoid(this)
        }
    })
}
