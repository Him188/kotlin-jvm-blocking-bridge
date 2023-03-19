import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import kotlin.coroutines.suspendCoroutine
import kotlin.jvm.JvmSynthetic

@JvmBlockingBridge
interface A {

    suspend fun member() {
        suspendCoroutine<Unit> {}

    }

    class A {

        suspend fun member() {
            suspendCoroutine<Unit> {}

        }
    }
}

interface Override : A {
    override suspend fun member() {
        suspendCoroutine<Unit> {}
    }
}