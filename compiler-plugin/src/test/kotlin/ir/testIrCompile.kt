package ir

import compile
import org.intellij.lang.annotations.Language
import kotlin.test.assertEquals

fun testIrCompile(
    @Language("kt")
    source: String
) {
    val result = compile(source, true)

    @Suppress("UNCHECKED_CAST")
    val test = result.classLoader.loadClass("TestData")
    assertEquals(
        "OK",
        (test.kotlin.objectInstance!!).run {
            this::class.java.methods.first { it.name == "main" }.invoke(this)
        } as String)
}
