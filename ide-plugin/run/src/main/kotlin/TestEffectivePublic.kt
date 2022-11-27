import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

@JvmBlockingBridge
object TestEffectivePublic {

    // private so no auto @JvmBlockingBridge
    private suspend fun privateFun() {}

    // effectively public so auto @JvmBlockingBridge
    @PublishedApi
    internal suspend fun effectivePublic() {
    }

    @JvmStatic
    fun main(args: Array<String>) {

    }
}