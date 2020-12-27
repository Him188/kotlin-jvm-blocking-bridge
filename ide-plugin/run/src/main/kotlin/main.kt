@file:Suppress("RedundantSuspendModifier")

import net.mamoe.kjbb.JvmBlockingBridge
import java.io.IOException
import kotlin.jvm.Throws

fun main() {

}

open class AClass {

    /**
     * K fun
     */
    @JvmBlockingBridge
    open suspend fun member() {
    }

    companion object {

        @JvmStatic
        @JvmBlockingBridge
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
