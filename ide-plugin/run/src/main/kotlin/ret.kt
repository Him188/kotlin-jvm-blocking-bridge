import net.mamoe.kjbb.JvmBlockingBridge

object TestRet {
    @JvmBlockingBridge
    suspend fun test(): String = ""
}