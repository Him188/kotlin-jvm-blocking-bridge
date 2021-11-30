package me.him188.kotlin.jvm.blocking.bridge

/**
 * Instructs the compiler to generate a blocking bridge for calling suspend function from Java.
 *
 * [JvmOverloads] and [JvmStatic] are supported.
 *
 * Example:
 * ```
 * @JvmBlockingBridge
 * suspend fun foo( params ) { /* ... */ }
 *
 * // The compiler generates (visible only from Java):
 * @GeneratedBlockingBridge
 * fun foo( params ) = `$runSuspend$` { foo(params) }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
public actual annotation class JvmBlockingBridge actual constructor()