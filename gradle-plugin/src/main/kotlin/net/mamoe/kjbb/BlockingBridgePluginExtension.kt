package net.mamoe.kjbb

import net.mamoe.kjbb.compiler.UnitCoercion
import net.mamoe.kjbb.compiler.extensions.IBridgeConfiguration

open class BlockingBridgePluginExtension : IBridgeConfiguration {
    /**
     * Strategy on mapping from `Unit` to `void` in JVM backend.
     *
     * Defaults [UnitCoercion.VOID].
     *
     * For compatibility with version lower than 1.7, use [UnitCoercion.COMPATIBILITY]
     *
     * @see UnitCoercion
     * @since 1.7
     */
    override var unitCoercion: UnitCoercion = UnitCoercion.DEFAULT

    /**
     * Generate blocking bridges for all effectively public suspend functions in the module where possible,
     * even if they are not annotated with @JvmBlockingBridge.
     *
     * @since 1.10
     */
    override var enableForModule: Boolean = false

    // var enabled: Boolean = true
}