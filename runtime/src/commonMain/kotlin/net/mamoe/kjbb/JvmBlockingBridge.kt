@file:OptIn(ExperimentalMultiplatform::class)

package net.mamoe.kjbb

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

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
@OptionalExpectation
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public expect annotation class JvmBlockingBridge()