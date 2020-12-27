import net.mamoe.kjbb.JvmBlockingBridge

open class TestRet {
    @JvmBlockingBridge
    open suspend fun test(): String = ""
}