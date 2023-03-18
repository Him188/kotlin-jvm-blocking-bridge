package me.him188.kotlin.jvm.blocking.bridge.ide

import me.him188.kotlin.jvm.blocking.bridge.compiler.extensions.IBridgeConfiguration

class BridgeModuleCacheService {
    var compilerEnabled: Boolean = true
    var config: IBridgeConfiguration = IBridgeConfiguration.Default

    var initialized = false
}