package jvm

import compile
import org.intellij.lang.annotations.Language
import kotlin.reflect.full.createInstance
import kotlin.test.assertEquals

fun testJvmCompile(
    @Language("kt")
    kt: String,
    @Language("java")
    java: String? = null
) {
    val result = compile(kt, java, false)

    @Suppress("UNCHECKED_CAST")
    val test = result.classLoader.loadClass("TestData")
    assertEquals(
        "OK",
        (test.kotlin.objectInstance ?: test.kotlin.createInstance()).run {
            this::class.java.methods.first { it.name == "main" }.invoke(this)
        } as String)
}
