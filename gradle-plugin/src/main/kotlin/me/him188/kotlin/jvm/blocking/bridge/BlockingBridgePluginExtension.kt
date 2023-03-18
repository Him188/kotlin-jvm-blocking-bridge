package me.him188.kotlin.jvm.blocking.bridge

import me.him188.kotlin.jvm.blocking.bridge.compiler.extensions.IBridgeConfiguration

open class BlockingBridgePluginExtension : IBridgeConfiguration {
    /**
     * Generate blocking bridges for all effectively public suspend functions in the module where possible,
     * even if they are not annotated with @JvmBlockingBridge.
     *
     * @since 1.10
     */
    override var enableForModule: Boolean = false

    // var enabled: Boolean = true
}