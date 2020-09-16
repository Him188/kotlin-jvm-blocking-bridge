package net.mamoe.kjbb.ide

import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeDeclarationChecker
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

class JvmBlockingBridgeIDEContainerContributor : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor,
    ) {
        container.useInstance(BlockingBridgeDeclarationChecker(false)) // TODO: 2020/9/16 How to check which compiler is in use?
    }
}