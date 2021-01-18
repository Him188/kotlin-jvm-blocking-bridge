package net.mamoe.kjbb

import net.mamoe.kjbb.compiler.UnitCoercion

open class BlockingBridgePluginExtension {
    /**
     * Strategy on mapping from `Unit` to `void` in JVM backend.
     *
     * Defaults [UnitCoercion.VOID].
     *
     * For compatibility with version lower than 1.7, use [UnitCoercion.COMPATIBILITY]
     *
     * @see UnitCoercion
     */
    var unitCoercion: UnitCoercion = UnitCoercion.DEFAULT

    // var enabled: Boolean = true
}