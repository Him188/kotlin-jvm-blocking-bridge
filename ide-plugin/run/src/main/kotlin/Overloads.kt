import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

@JvmBlockingBridge
object X {
    @JvmStatic
    @JvmOverloads
    suspend fun test(s: String, v: Int = 1) {
    }
} 