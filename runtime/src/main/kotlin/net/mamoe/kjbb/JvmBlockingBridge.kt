package net.mamoe.kjbb

/**
 * Instructs the compiler to generate a blocking bridge for calling suspend function from Java.
 *
 * Example:
 * ```
 * @JvmBlockingBridge
 * suspend fun foo( params ) { /* ... */ }
 *
 * // The compiler generates (visible only for Java):
 * @JvmBlockingBridge
 * fun foo( params ) = runBlocking { foo(params) }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class JvmBlockingBridge