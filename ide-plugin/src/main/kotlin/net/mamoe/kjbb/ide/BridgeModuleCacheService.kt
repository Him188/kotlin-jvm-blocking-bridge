package net.mamoe.kjbb.ide

import net.mamoe.kjbb.compiler.extensions.IBridgeConfiguration

class BridgeModuleCacheService {
    var compilerEnabled: Boolean = true
    var isIr: Boolean = false
    var config: IBridgeConfiguration = IBridgeConfiguration.Default

    var initialized = false
}