import kotlin.coroutines.suspendCoroutine

fun main() {
    println(greeting("js"))
}

fun greeting(name: String) =
    "Hello, $name"

suspend fun js() {
    suspendCoroutine<Unit> { }
}

interface JsInterface {
    suspend fun jsInterfaceMember() {
        suspendCoroutine<Unit> { }
    }
}