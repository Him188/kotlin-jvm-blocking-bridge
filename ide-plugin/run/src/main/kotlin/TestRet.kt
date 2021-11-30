import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

open class TestRet2 {
    @JvmBlockingBridge
    @JvmOverloads
    open suspend fun test(ii: Int = 1): String = ""
}
