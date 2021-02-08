import net.mamoe.kjbb.JvmBlockingBridge

@JvmBlockingBridge
interface TestAnnotationsOnClass {

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

        suspend fun test(s: String) {

        }

        internal suspend fun test2() {

        }

        @PublishedApi
        internal suspend fun test3() {

        }

        @JvmBlockingBridge
        class Inner {

            suspend fun test() {

            }
        }
    }
}

interface TestAnnotationsOnClass2 : TestAnnotationsOnClass
interface TestAnnotationsOnClass21 : TestAnnotationsOnClass
interface TestAnnotationsOnClass211 : TestAnnotationsOnClass
interface TestAnnotationsOnClass2111 : TestAnnotationsOnClass
interface TestAnnotationsOnClass21111 : TestAnnotationsOnClass