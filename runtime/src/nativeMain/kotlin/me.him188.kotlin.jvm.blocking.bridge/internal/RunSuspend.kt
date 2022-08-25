package me.him188.kotlin.jvm.blocking.bridge.internal

import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

/**
 * me/him188/kotlin/jvm/blocking/bridge/internal/RunSuspendKt.runSuspend
 */
@Deprecated("For compiler use only", level = DeprecationLevel.HIDDEN)
public fun runSuspend(block: suspend () -> Any?): Any? {
    return runBlocking(EmptyCoroutineContext) { block() }
}