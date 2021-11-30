@file:Suppress("RedundantSuspendModifier")

import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import java.io.IOException
import kotlin.jvm.Throws

fun main() {

}

open class AClass {

    @JvmBlockingBridge
    @JvmOverloads
    open suspend fun overloads(x: Int = 1, s: String = "") {

    }

    fun overloads(x: Int) {

    }


    /**
     * K fun
     */
    @JvmBlockingBridge
    open suspend fun member() {
    }

    companion object {

        @JvmStatic
        @JvmBlockingBridge
        @Deprecated("")
        suspend fun comp() {
        }

        @JvmStatic
        fun f() {
        }
    }

    object P {

        @JvmStatic
        @JvmBlockingBridge
        @Throws(IOException::class)
        suspend fun compThrows() {
        }

        @JvmStatic
        fun f() {
        }
    }
}
