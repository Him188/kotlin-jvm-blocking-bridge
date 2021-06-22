@file:JvmBlockingBridge

import net.mamoe.kjbb.JvmBlockingBridge

interface TestAnnotationsOnClass {


    /**
     * x
     */
    @JvmSynthetic
    suspend fun test() {

    }

    object X {

        suspend fun test() {

        }

        @JvmOverloads
        suspend fun test(s: String, v: Int = 1) {

        }

        internal suspend fun test2() {

        }

        @PublishedApi
        internal suspend fun test3() {

        }

        @JvmBlockingBridge
        class Inner {

            @JvmSynthetic
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