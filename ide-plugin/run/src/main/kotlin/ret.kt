import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

open class TestRet {
    @JvmBlockingBridge
    @MyDeprecated("asd")
    open suspend fun deprecatedSuspendMember(): String = ""

    @JvmBlockingBridge
    suspend fun suspendMember(): String = ""
}

typealias MyDeprecated = Deprecated