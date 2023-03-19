import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import kotlin.coroutines.suspendCoroutine

@JvmBlockingBridge
suspend fun fn() {
    suspendCoroutine<Unit> {  }
}

