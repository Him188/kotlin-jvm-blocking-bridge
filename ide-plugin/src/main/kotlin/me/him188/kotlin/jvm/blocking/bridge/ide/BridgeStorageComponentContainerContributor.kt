package me.him188.kotlin.jvm.blocking.bridge.ide

import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.jvm.hasJvmComponent
import me.him188.kotlin.jvm.blocking.bridge.compiler.diagnostic.BlockingBridgeDeclarationChecker
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.descriptorUtil.module


class BridgeStorageComponentContainerContributor : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor,
    ) {
        // container.getService(JvmBlockingBridgeConfigurationService::class.java).ext = ext ?: error("Failed to get ext service")

        //container.useInstance(BridgeCodegenCliExtension())

        if (!platform.hasJvmComponent()) return

        container.useInstance(object :
            BlockingBridgeDeclarationChecker(moduleDescriptor.isIr(),
                { it.bridgeConfiguration }) {
            override fun isPluginEnabled(
                descriptor: DeclarationDescriptor,
            ): Boolean {
                return descriptor.module.isBlockingBridgePluginEnabled()
            }
        })
    }
}