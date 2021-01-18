import net.mamoe.kjbb.JvmBlockingBridge

open class TestRet2 {
    @JvmBlockingBridge
    @JvmOverloads
    open suspend fun test(ii: Int = 1): String = ""
}
