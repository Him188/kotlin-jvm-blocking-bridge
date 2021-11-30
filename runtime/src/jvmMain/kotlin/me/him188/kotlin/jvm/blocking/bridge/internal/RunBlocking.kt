@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "FunctionName", "unused")
@file:JvmName("RunBlockingKt")

package me.him188.kotlin.jvm.blocking.bridge.internal

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * me/him188/kotlin/jvm/blocking/bridge/internal/RunSuspendKt.runSuspend
 */
@Deprecated("For compiler use only", level = DeprecationLevel.HIDDEN)
// don't internal, otherwise function name will be changed
public fun `$runSuspend$`(block: suspend () -> Any?): Any? {
    val run = RunSuspend<Any?>()
    block.startCoroutine(run)
    return run.await()
}

internal class RunSuspend<R> : Continuation<R> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    @Suppress("RESULT_CLASS_IN_RETURN_TYPE")
    var result: Result<R>? = null

    override fun resumeWith(result: Result<R>) = synchronized(this) {
        this.result = result
        (this as Object).notifyAll()
    }

    fun await(): R {
        synchronized(this) {
            while (true) {
                when (val result = this.result) {
                    null -> (this as Object).wait()
                    else -> {
                        return result.getOrThrow()
                    }
                }
            }
        }
    }
}
