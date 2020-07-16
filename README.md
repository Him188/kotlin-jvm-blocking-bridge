# kotlin-jvm-blocking-bridge

**[CHINESE 简体中文](./README-chs.md)**

Kotlin compiler plugin that can generate a blocking bridge for calling suspend functions from Java with minimal effort

### Intention
Kotlin suspend function is compiled with an additional `$completion: Continuation` parameter, making it hard to call from Java, so we often make extra effort to simplify calling.

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

However, there several problems:
- KDoc is copied to the bridge, when updating, copying is also required.
- Changing the signature becomes inconvenient.
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
```kotlin
@JvmBlockingBridge
suspend fun downloadImage(): Image
```
With the help of the compiler plugin, `fun downloadImage()` is generated directly without `@JvmName`  
With the help of the IDE plugin, `suspend fun downloadImage` is not visible to Java callers, and they reference to the generated blocking bridge method with the same facade method signature as the `suspend fun`


## Modules
- **runtime-library**  *provides @JvmBlockingBridge annotation*
- **compiler-plugin**  *provides bridge generators, supporting current JVM backend and experimental IR backend*
- **ide-plugin**  *for IntelliJ platform IDEs only*

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
- Hide suspend functions annotated with `@JvmBlockingBridge` from Java
- Add reference resolution for generated bridge functions for Java
- Kotlin callers can't reference to bridge functions (if so, error 'unresolved reference' will be reported by the compiler)

## Requirements
- Gradle (`6.0` or higher recommended)
- Kotlin `1.4-M3`, `1.4-RC`, `1.4.0` or higher
- IntelliJ IDEA 或 Android Studio (newest version recommended)

## WIP
This project is working in progress.   
TODOs:
- Bridge generating in IR backend (experimental since Kotlin 1.3)  
  *Available now.*
- Bridge generation in the legacy JVM backend (generally used):
  *Available now.*
- IDE plugin for resolving

## Try X
now

You can try it now with Kotlin Compiler IR backend.

1. **Install Gradle plugin.**

`build.gradle` or `build.gradle.kts`
```kotlin
plugins {
  id("net.mamoe.kotlin-jvm-blocking-bridge") version "0.1.12"
}
```

If gradle can't resolve plugin, please add `gradlePluginPortal()` into `settings.gradle` or `settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
```

The plugin will automatically install runtime annotation library for you, as:
```kotlin
implementation("net.mamoe:kotlin-jvm-blocking-bridge")
```
Therefore, you need only to install the plugin.

2. (optional) **Switch to IR backend.**

IR compiler backend is **experimental**. If you meet compilation error when using default JVM backend, please switch to IR backend, otherwise, it's not recommend to do so.

Add into `build.gradle` or `build.gradle.kts`
```kotlin
tasks.withType<KotlinCompile> {
    kotlinOptions.useIR = true
}
```
