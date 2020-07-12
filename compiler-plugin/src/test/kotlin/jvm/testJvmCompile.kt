package jvm

import compile
import org.intellij.lang.annotations.Language
import kotlin.test.assertEquals

fun testJvmCompile(
    @Language("kt")
    source: String
) {
    val result = compile(source, false)

    @Suppress("UNCHECKED_CAST")
    val test = result.classLoader.loadClass("TestData")
    assertEquals(
        "OK",
        (test.kotlin.objectInstance!!).run {
            this::class.java.methods.first { it.name == "main" }.invoke(this)
        } as String)
}
