package net.mamoe.kjbb


private const val message = "This is generated to help Java callers, don't use in Kotlin."

/**
 * The annotation that is added to the generated JVM blocking bridges by the compiler
 * to help IntelliJ plugin to hide members correctly.
 */
@Deprecated(message, level = DeprecationLevel.HIDDEN)
@RequiresOptIn(message, level = RequiresOptIn.Level.ERROR)
internal annotation class GeneratedBlockingBridge