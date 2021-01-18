package net.mamoe.kjbb.compiler

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object JvmBlockingBridgeCompilerConfigurationKeys {
    private const val NAMESPACE = "kjbb"

    @JvmStatic
    val UNIT_COERCION = CompilerConfigurationKey<String>("$NAMESPACE.unit-coercion")
}