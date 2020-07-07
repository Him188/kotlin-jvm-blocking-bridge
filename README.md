# kotlin-jvm-blocking-bridge
Kotlin compiler plugin that can generate a blocking bridge for calling suspend functions from Java with minimal effort

### Intention
Kotlin suspend function is compiled with an additional `$completion: Continuation` parameter, making it hard to call from Java.

E.g., for
```kotlin
suspend fun downloadImage(): Image
```
We often write
```kotlin
@JvmName("downloadImage") // avoid resolution ambiguity
fun downloadImageBlocking(): Image = runBlocking { downloadImage() }
```
... additionally for Java callers.

However, there are two problems:
- We don't want `downloadImageBlocking` to be exposed to Kotlin callers, but we can't hide them.  
  Workaround:
  ```kotlin
  @RequiresOptIn(level = ERROR)
  annotation class JavaFriendlyApi
  
  @JavaFriendlyApi // so IDE reports 'Experimental API usage' error for calling from Kotlin.
  @JvmName("downloadImage") // avoid resolution ambiguity
  fun downloadImageBlocking(): Image = runBlocking { downloadImage() }
  ```
  We repeat this boilerplate too many times.
- We don't want also expose `suspend` function to Java callers,  
  so we add `@JvmSynthetic`, getting the final code:
  ```kotlin
  @JvmSynthetic
  suspend fun downloadImage(): Image

  @RequiresOptIn(level = ERROR)
  annotation class JavaFriendlyApi
  
  @JavaFriendlyApi // so IDE reports 'Experimental API usage' error for calling from Kotlin.
  @JvmName("downloadImage") // avoid resolution ambiguity
  fun downloadImageBlocking(): Image = runBlocking { downloadImage() }
  ```

This plugin has been designed to minimize work against Java compatibility:
```
@JvmBlockingBridge
suspend fun downloadImage(): Image
```
With the help of the compiler plugin, `fun downloadImage()` is generated directly without `@JvmName`  
With the help of the IDE plugin, `suspend fun downloadImage` is not visible to Java callers, and they reference to the generated blocking bridge method with the same facade method signature as the `suspend fun`


## Modules
- **compiler-plugin**  *provides bridge generators, supporting current JVM backend and experimental IR backend*
- **ide-plugin** plugin  *for IntelliJ platform IDEs only*
- **runtime-library**  *provides @JvmBlockingBridge annotation*

### Runtime library

- Provides `public annotation class net.mamoe.kjbb.JvmBlockingBridge`
- Provides `internal annotation class net.mamoe.kjbb.GeneratedBlockingBridge` that is added implicitly to generated bridges.

### Compiler plugin

Given Kotlin `suspend` function:
```kotlin
@JvmBlockingBridge
suspend fun test(a1: Int, a2: Any): String
```

This plugin generates the non-suspend bridge function with the same signature (visible only from Java)
```kotlin
@GeneratedBlockingBridge
fun test(a1: Int, a2: Any): String = runBlocking { test(a1, a2) }
```

### IDE plugin

- Adds compiler-plugin dependency inspection when using `@JvmBlockingBridge`
- Hide suspend functions annotated with `@JvmBlockingBridge` from Kotlin
- Add reference resolution for generated bridge functions for Java

