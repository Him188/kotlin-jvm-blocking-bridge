@file:OptIn(ExperimentalMultiplatform::class)

package me.him188.kotlin.jvm.blocking.bridge

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
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
public expect annotation class JvmBlockingBridge()