package net.mamoe.kjbb.ir

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.*


private class JBBIrCallTransformer(
    val context: IrPluginContext
) : IrElementTransformerVoid(), ClassLoweringPass {
    val newDeclarations = mutableListOf<IrDeclaration>()

    override fun visitFunction(declaration: IrFunction): IrStatement {

        if (declaration.descriptor.annotations.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME))
            newDeclarations.addAll(context.lowerOriginFunction(declaration).orEmpty())
        return declaration
    }

    override fun lower(irClass: IrClass) {
        irClass.transformChildrenVoid(this)
        //context.lowerOriginFunction(declaration)

        val (classes, functions) = newDeclarations.partition { it is IrClass }
//        classes.forEach(irClass.file::addChild)
        functions.forEach(irClass::addMember)
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
