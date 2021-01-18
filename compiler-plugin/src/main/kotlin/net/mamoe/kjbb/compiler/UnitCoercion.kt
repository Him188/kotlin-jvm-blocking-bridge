package net.mamoe.kjbb.compiler

/**
 * Strategy on mapping from `Unit` to `void` in JVM backend.
 */
enum class UnitCoercion {
    /**
     * Generates all bridges with return type [VOID].
     */
    VOID,

    /**
     * Generates all bridges twice, one with return type [VOID] and another with return type [Unit].
     *
     * This option provides binary compatibility with classes generated using old (before v1.7) plugin versions.
     */
    COMPATIBILITY,
    ;

    companion object {
        @JvmStatic
        val DEFAULT = VOID
    }
}