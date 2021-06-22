import net.mamoe.kjbb.JvmBlockingBridge

@JvmBlockingBridge
object X {
    @JvmStatic
    @JvmOverloads
    suspend fun test(s: String, v: Int = 1) {
    }
} 