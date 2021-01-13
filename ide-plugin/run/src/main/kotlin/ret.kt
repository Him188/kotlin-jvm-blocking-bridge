import net.mamoe.kjbb.JvmBlockingBridge

open class TestRet {
    @JvmBlockingBridge
    @MyDeprecated("asd")
    @JvmSynthetic
    open suspend fun test(): String = ""
}

typealias MyDeprecated = Deprecated