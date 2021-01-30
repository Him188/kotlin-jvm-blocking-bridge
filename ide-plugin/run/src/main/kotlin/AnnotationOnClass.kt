import net.mamoe.kjbb.JvmBlockingBridge

@JvmBlockingBridge
object TestAnnotationsOnClass {

    /**
     * x
     */
    @JvmBlockingBridge // explicitly
    suspend fun test() {

    }

    @JvmBlockingBridge
    object X {

        suspend fun test() {

        }
    }
}