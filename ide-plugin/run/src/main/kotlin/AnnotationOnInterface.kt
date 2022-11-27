import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

@JvmBlockingBridge
interface AnnotationOnInterface {

    //
    suspend fun suspendMember() {

    }

    @JvmSynthetic
    suspend fun synthetic() {
    }
}