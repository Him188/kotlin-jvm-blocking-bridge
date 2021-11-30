import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

open class TestRet {
    @JvmBlockingBridge
    @MyDeprecated("asd")
    open suspend fun test(): String = ""
}

typealias MyDeprecated = Deprecated