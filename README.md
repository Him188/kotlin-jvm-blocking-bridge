# kotlin-jvm-blocking-bridge

**[CHINESE 简体中文](./README-chs.md)**

Kotlin compiler plugin for generating blocking bridges for calling suspend functions from Java with minimal effort

## Screenshots
<details>
<summary>Click to expand</summary>

Kotlin suspend functions:  
![image_2.png](https://i.loli.net/2020/08/08/d5cYwhQqeuj8Nvf.png)

Bridge method calls:  
![image.png](https://i.loli.net/2020/08/08/tJyGeOcB8E4muQ5.png)

Documentation and navigation support:  
![image_1](https://i.loli.net/2020/08/08/koCl6zj4OAJ5aUN.png)
</details>

### Motivation
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

**Read specifications on [BridgeFeatures.md](BridgeFeatures.md)**

### Runtime library

- Provides `public annotation class net.mamoe.kjbb.JvmBlockingBridge`
- Provides `internal annotation class net.mamoe.kjbb.GeneratedBlockingBridge` that is added implicitly to generated bridges.
- Provides some internal functions used by generated bridges.

**Important Note**: the runtime library contains not only the annotations, but also coroutine runner functions which is required by the compiler plugin.  
Therefore, you should not exclude the runtime library in shadowed jars (if your project uses so) or when running your application.

### Compiler plugin

Given Kotlin `suspend` function:
```kotlin
@JvmBlockingBridge
suspend fun test(a1: Int, a2: Any): String
```

This plugin generates the non-suspend bridge function with the same signature (visible only from Java)
```kotlin
@GeneratedBlockingBridge
fun test(a1: Int, a2: Any): String = runBlocking { 
    test(a1, a2) // calls the original suspend `test` 
} // runBlocking is for demonstration. KJBB compiles your code in a smart way and doesn't require kotlinx-coroutines-core. 
```

### IDE plugin

- Adds compiler-plugin dependency inspection when using `@JvmBlockingBridge`
- Hide suspend functions annotated with `@JvmBlockingBridge` from Java
- Add reference resolution for generated bridge functions for Java
- Kotlin callers can't reference to bridge functions (if so, error 'unresolved reference' will be reported by the compiler)

## Requirements
- Gradle (`6.0` or higher recommended)
- **Kotlin `1.4.20` or higher**
- IntelliJ IDEA or Android Studio (newer version recommended)

## Try now

The plugin is ready to use.

### Library users
If you use a library that uses Kotlin Jvm Blocking Bridge, you need to install the IntelliJ plugin.

#### **Install IntelliJ IDEA (or Android Studio) plugin**
   The plugin currently supports from 2019.\* to 2020.\*  
   It's strongly recommended using the latest IJ or AS, you may update using [JetBrains ToolBox](https://www.jetbrains.com/toolbox-app/)  
   Please note that Eclipse and Visual Studio aren't supported.

   One-key install: [Install to IntelliJ IDEA](https://plugins.jetbrains.com/embeddable/install/14816)

   You can also install manually:

   1. Open `File->Settings->Plugins->Marketplace` in your IDE
   2. Search `Kotlin Jvm Blocking Bridge`, download and install
   3. Restart your IDE

### Library authors
If you're developing a library with KJBB, you need to install the Gradle plugin additionally.

#### **Install Gradle plugin**

`build.gradle` or `build.gradle.kts`
```kotlin
plugins {
  id("net.mamoe.kotlin-jvm-blocking-bridge") version "1.5.0"
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

The plugin will automatically install runtime library for you, as:
```kotlin
implementation("net.mamoe:kotlin-jvm-blocking-bridge:1.5.0")
```
Therefore, you need only to install the plugin, and the compiler plugin will work finely.

## Supported compiler backends

Kotlin compiler has two backends, one of which, `JVM`, is the legacy one and is going to be replaced with the new one, named `IR` (Internal Representation).  
Currently, legacy `JVM` backend is used by default since it's more stable than the `IR`.

This plugin supports both of them.

**If you meet compilation error when using default JVM backend, please switch to IR backend, otherwise, it's not recommended doing so.**

To switch to IR backend, add into `build.gradle` or `build.gradle.kts`:
```kotlin
tasks.withType<KotlinCompile> {
    kotlinOptions.useIR = true
}
```


## Kotlin mulitplatform projects (MPP)

KJBB compiler and gradle plugins supports Kotlin MPP 'as is'.

The runtime library supports all MPP targets.

The runtime library is publish in the new layout came from Kotlin 1.4.0, and the dependency is set up auto matically by the gradle plugin.
