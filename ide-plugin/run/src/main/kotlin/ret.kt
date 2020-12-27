import net.mamoe.kjbb.JvmBlockingBridge

open class TestRet {
    @JvmBlockingBridge
    @Deprecated("asd")
    open suspend fun test(): String = ""
}