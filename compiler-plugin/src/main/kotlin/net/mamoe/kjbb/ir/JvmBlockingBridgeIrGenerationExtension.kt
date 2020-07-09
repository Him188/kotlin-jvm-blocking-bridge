package net.mamoe.kjbb.ir

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * For IR backend.
 */
class JvmBlockingBridgeIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        for (file in moduleFragment.files) {
            JvmBlockingBridgeLoweringPass(pluginContext).runOnFileInOrder(file)
        }
    }
}

internal fun ClassLoweringPass.runOnFileInOrder(irFile: IrFile) {
    irFile.acceptChildrenVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            lower(declaration) // lower bridge before lowering suspend
        }
    })
}
