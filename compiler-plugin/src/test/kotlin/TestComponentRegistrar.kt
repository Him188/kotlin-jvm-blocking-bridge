import net.mamoe.kjbb.compiler.backend.jvm.BridgeCodegen
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeDeclarationChecker
import net.mamoe.kjbb.compiler.extensions.JvmBlockingBridgeIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

@Suppress("unused")
open class TestComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {

        StorageComponentContainerContributor.registerExtension(project, object : StorageComponentContainerContributor {
            override fun registerModuleComponents(
                container: StorageComponentContainer,
                platform: TargetPlatform,
                moduleDescriptor: ModuleDescriptor,
            ) {
                container.useInstance(BlockingBridgeDeclarationChecker())
            }
        })
        IrGenerationExtension.registerExtension(project, JvmBlockingBridgeIrGenerationExtension())
        ExpressionCodegenExtension.registerExtension(project, object : ExpressionCodegenExtension {
            override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
                BridgeCodegen(codegen).generate()
            }

            override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
                get() = true
        })
    }
}


