package me.him188.kotlin.jvm.blocking.bridge.ide.fix

import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.kotlin.util.AbstractKotlinBundle


@NonNls
private const val BUNDLE = "messages.BlockingBridgeBundle"

object BlockingBridgeBundle : AbstractKotlinBundle(BUNDLE) {
    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)

    @JvmStatic
    fun htmlMessage(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params).withHtml()

    @JvmStatic
    fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): () -> String =
        { getMessage(key, *params) }
}
