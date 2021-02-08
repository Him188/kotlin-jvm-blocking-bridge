package net.mamoe.kjbb.compiler.backend.ir

import net.mamoe.kjbb.compiler.backend.jvm.BlockingBridgeAnalyzeResult
import net.mamoe.kjbb.compiler.backend.jvm.followedBy
import net.mamoe.kjbb.compiler.extensions.IJvmBlockingBridgeCodegenJvmExtension
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.utils.addToStdlib.cast

/**
 * For top-level functions
 */
class JvmBlockingBridgeFileLoweringPass(
    private val context: IrPluginContext,
    private val ext: IJvmBlockingBridgeCodegenJvmExtension,
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformDeclarationsFlat { declaration ->
            declaration.transformFlat(context, ext)
        }
    }
}

internal fun IrDeclaration.transformFlat(
    context: IrPluginContext,
    ext: IJvmBlockingBridgeCodegenJvmExtension,
): List<IrDeclaration> {
    val declaration = this
    if (declaration is IrSimpleFunction) {
        if (declaration.isGeneratedBlockingBridgeStub())
            return listOf()

        val capability: BlockingBridgeAnalyzeResult =
            declaration.analyzeCapabilityForGeneratingBridges(ext)
        capability.createDiagnostic()?.let { diagnostic ->
            DiagnosticSink.THROW_EXCEPTION.report(diagnostic)
        }

        if (capability.shouldGenerate) {
            return declaration.followedBy(context.generateJvmBlockingBridges(declaration))
        }
    }

    return listOf(declaration)
}

/**
 * For in-class functions
 */
class JvmBlockingBridgeClassLoweringPass(
    private val context: IrPluginContext,
    private val ext: IJvmBlockingBridgeCodegenJvmExtension,
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        irClass.transformDeclarationsFlat { declaration ->
            declaration.transformFlat(context, ext)
        }
        irClass.companionObject()?.cast<IrClass>()?.let(::lower)
    }
}