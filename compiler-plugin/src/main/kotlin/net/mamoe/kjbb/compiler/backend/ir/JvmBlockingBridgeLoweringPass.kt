package net.mamoe.kjbb.compiler.backend.ir

import net.mamoe.kjbb.compiler.backend.jvm.followedBy
import net.mamoe.kjbb.compiler.extensions.isGeneratedBlockingBridgeStub
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat

class JvmBlockingBridgeLoweringPass(
    private val context: IrPluginContext
) : ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        irClass.transformDeclarationsFlat { declaration ->
            if (declaration is IrSimpleFunction) {
                if (declaration.descriptor.isGeneratedBlockingBridgeStub())
                    return@transformDeclarationsFlat listOf()

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
                        return@transformDeclarationsFlat listOf(declaration)
                    }
                    check(!declaration.hasDuplicateBridgeFunction()) {
                        // TODO: 2020/7/8 DIAGNOSTICS FROM PLATFORM_DECLARE_CLASH
                        "PLATFORM_DECLARE_CLASH: function '${declaration.name}'"
                    }
                    return@transformDeclarationsFlat declaration.followedBy(
                        context.generateJvmBlockingBridges(
                            declaration
                        )
                    )
                }
            }

            listOf(declaration)
        }
    }
}