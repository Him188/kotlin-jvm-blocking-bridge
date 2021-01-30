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

        internal suspend fun test2() {

        }

        @PublishedApi
        internal suspend fun test3() {

        }
    }
}