import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

@JvmBlockingBridge
interface AnnotationOnInterface {

    //
    fun nonSuspend() {

    }

    @JvmSynthetic
    suspend fun suspend() {
    }
}