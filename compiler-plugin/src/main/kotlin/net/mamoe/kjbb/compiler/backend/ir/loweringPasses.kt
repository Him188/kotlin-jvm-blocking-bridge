package net.mamoe.kjbb.compiler.backend.ir

import net.mamoe.kjbb.compiler.backend.jvm.BlockingBridgeAnalyzeResult
import net.mamoe.kjbb.compiler.backend.jvm.followedBy
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat

/**
 * For top-level functions
 */
class JvmBlockingBridgeFileLoweringPass(
    private val context: IrPluginContext,
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformDeclarationsFlat { declaration ->
            declaration.transformFlat(context)
        }
    }
}

internal fun IrDeclaration.transformFlat(context: IrPluginContext): List<IrDeclaration> {
    val declaration = this
    if (declaration is IrSimpleFunction) {
        if (declaration.isGeneratedBlockingBridgeStub())
            return listOf()

        if (declaration.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)) {
            val capability: BlockingBridgeAnalyzeResult =
                declaration.analyzeCapabilityForGeneratingBridges()
            capability.createDiagnostic()?.let { diagnostic ->
                DiagnosticSink.THROW_EXCEPTION.report(diagnostic)
            }

            if (capability.shouldGenerate) {
                return declaration.followedBy(context.generateJvmBlockingBridges(declaration))
            }
        }
    }

    return listOf(declaration)
}

/**
 * For in-class functions
 */
class JvmBlockingBridgeClassLoweringPass(
    private val context: IrPluginContext,
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        irClass.transformDeclarationsFlat { declaration ->
            declaration.transformFlat(context)
        }
    }
}