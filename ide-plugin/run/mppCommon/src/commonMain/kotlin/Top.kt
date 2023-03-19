@file:JvmBlockingBridge

import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import kotlin.coroutines.suspendCoroutine

/**
 * OK
 */
suspend fun topLevel() {
    suspendCoroutine<Unit> {}
}
