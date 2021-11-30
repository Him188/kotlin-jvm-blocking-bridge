@file:JvmBlockingBridge

import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge

suspend fun test() {

}

@JvmBlockingBridge // should error
suspend fun test2() {

}