package me.him188.kotlin.jvm.blocking.bridge.compiler.extensions

import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.ir.JvmBlockingBridgeClassLoweringPass
import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.ir.JvmBlockingBridgeFileLoweringPass
import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.ir.PlatformIntrinsics
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative

/**
 * For IR backend.
 */
// @AutoService(IrGenerationExtension::class)
open class JvmBlockingBridgeIrGenerationExtension(
    private val ext: IBridgeConfiguration,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val platform = moduleFragment.descriptor.platform!!

        val intrinsics = when {
            platform.isJvm() -> PlatformIntrinsics.ForJvm
            platform.isNative() -> PlatformIntrinsics.ForNative
            else -> return
        }

        for (file in moduleFragment.files) {
            JvmBlockingBridgeClassLoweringPass(pluginContext, ext, intrinsics).runOnFileInOrder(file)
            JvmBlockingBridgeFileLoweringPass(pluginContext, ext, intrinsics).runOnFileInOrder(file)
        }
    }
}

internal fun ClassLoweringPass.runOnFileInOrder(irFile: IrFile) {
    irFile.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            lower(declaration) // lower bridge before lowering suspend
        }
    })
}

internal fun FileLoweringPass.runOnFileInOrder(irFile: IrFile) {
    irFile.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFile(declaration: IrFile) {
            lower(declaration)
        }
    })
}
