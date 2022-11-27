@file:Suppress("RedundantSuspendModifier")

import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import java.io.IOException
import kotlin.jvm.Throws

fun main() {

}

open class AClass {

    @JvmBlockingBridge
    @JvmOverloads
    open suspend fun overloadsClash(x: Int = 1, s: String = "") {

    }

    fun overloadsClash(x: Int) { // should report an error but not implemented yet.

    }

    @JvmBlockingBridge
    @JvmOverloads
    open suspend fun overloads(x: Int = 1, s: String = "") {

    }


    /**
     * K fun
     */
    @JvmBlockingBridge
    open suspend fun suspendMember() {
    }

    companion object {

        @JvmStatic
        @JvmBlockingBridge
        @Deprecated("")
        suspend fun suspendStaticInCompanion() {
        }

        @JvmStatic
        fun ordinaryStaticInCompanion() {
        }
    }

    object P {

        @JvmStatic
        @JvmBlockingBridge
        @Throws(IOException::class)
        suspend fun suspendThrowsInObject() {
        }

    }
}
