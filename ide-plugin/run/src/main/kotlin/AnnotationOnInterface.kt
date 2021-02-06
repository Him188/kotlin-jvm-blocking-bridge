import net.mamoe.kjbb.JvmBlockingBridge

@JvmBlockingBridge
interface AnnotationOnInterface {

    //
    fun nonSuspend() {

    }

    @JvmSynthetic
    suspend fun suspend() {
    }
}