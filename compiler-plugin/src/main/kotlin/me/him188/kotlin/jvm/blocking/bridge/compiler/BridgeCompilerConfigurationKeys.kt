package me.him188.kotlin.jvm.blocking.bridge.compiler

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object JvmBlockingBridgeCompilerConfigurationKeys {
    @JvmStatic
    val ENABLE_FOR_MODULE =
        CompilerConfigurationKeyWithName<String>("enableForModule")
}

// don't data
class CompilerConfigurationKeyWithName<T>(val name: String) : CompilerConfigurationKey<T>(name)