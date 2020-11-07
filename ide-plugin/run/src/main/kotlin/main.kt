import net.mamoe.kjbb.JvmBlockingBridge

fun main() {

}

open class AClass {

    @JvmBlockingBridge
    open suspend fun member() {
    }

    companion object {

        @JvmStatic
        @JvmBlockingBridge
        open suspend fun comp() {
        }

        @JvmStatic
        fun f() {
        }
    }

    object P {

        @JvmStatic
        @JvmBlockingBridge
        open suspend fun comp() {
        }

        @JvmStatic
        fun f() {
        }
    }
}