package me.him188.kotlin.jvm.blocking.bridge.compiler.extensions

data class BridgeConfigurationImpl(
    override val enableForModule: Boolean,
) : IBridgeConfiguration

interface IBridgeConfiguration {
    val enableForModule: Boolean

    companion object {
        val Default = object : IBridgeConfiguration {
            override val enableForModule: Boolean get() = false
        }
    }
}
