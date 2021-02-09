package net.mamoe.kjbb.compiler

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object JvmBlockingBridgeCompilerConfigurationKeys {
    @JvmStatic
    val UNIT_COERCION = CompilerConfigurationKeyWithName<String>("unitCoercion")

    @JvmStatic
    val ENABLE_FOR_MODULE = CompilerConfigurationKeyWithName<String>("enableForModule")
}

// don't data
class CompilerConfigurationKeyWithName<T>(val name: String) : CompilerConfigurationKey<T>(name)