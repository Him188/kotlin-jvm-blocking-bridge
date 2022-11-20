
@JvmBlockingBridge
interface AnnotationOnInterface {

    //
    fun nonSuspend() {

    }

    @JvmSynthetic
    suspend fun suspend() {
    }
}