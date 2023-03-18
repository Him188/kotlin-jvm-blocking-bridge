package me.him188.kotlin.jvm.blocking.bridge.compiler.backend.ir

import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.resolve.BlockingBridgeAnalyzeResult
import me.him188.kotlin.jvm.blocking.bridge.compiler.extensions.IBridgeConfiguration
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

/**
 * For top-level functions
 */
class JvmBlockingBridgeFileLoweringPass(
    private val context: IrPluginContext,
    private val ext: IBridgeConfiguration,
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformDeclarationsFlat { declaration ->
            declaration.transformFlat(context, ext)
        }
    }
}

internal fun IrDeclaration.transformFlat(
    context: IrPluginContext,
    ext: IBridgeConfiguration,
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
    private val ext: IBridgeConfiguration,
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        irClass.transformDeclarationsFlat { declaration ->
            declaration.transformFlat(context, ext)
        }
        irClass.companionObject()?.let(::lower)
    }
}

internal fun <T> T?.followedBy(list: Collection<T>): List<T> {
    if (this == null) return list.toList()
    val new = ArrayList<T>(list.size + 1)
    new.add(this)
    new.addAll(list)
    return new
}
