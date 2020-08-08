# kotlin-jvm-blocking-bridge

**[CHINESE 简体中文](./README-chs.md)**

Kotlin compiler plugin for generating blocking bridges for calling suspend functions from Java with minimal effort

## Screenshots
Kotlin suspend functions:  
![image_2.png](https://i.loli.net/2020/08/08/d5cYwhQqeuj8Nvf.png)

Bridge method calls:  
![image.png](https://i.loli.net/2020/08/08/tJyGeOcB8E4muQ5.png)

Documentation and navigation support:  
![image_1](https://i.loli.net/2020/08/08/koCl6zj4OAJ5aUN.png)

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
- **Kotlin `1.4.0-rc`, `1.4.0` or higher**
- IntelliJ IDEA or Android Studio (newer version recommended)

## Try now

The plugin is ready to use.

1. **Check Kotlin version**

This plugin requires some new features published in Kotlin `1.4.0-rc`, therefore, please check your Kotlin compiler version.
  - In your `build.gradle.kts` or `build.gradle`,
      - If you use `plugins { }` DSL, please ensure `id("kotlin") version "1.4.0-rc"` or `kotlin("jvm") version "1.4.0-rc"`
      - If you use `buildscript { }` DSL, please ensure `classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0-rc")`
  - In your IDE, open `Tools->Kotlin->Configure Kotlin Plugin Updates`, switch 'Update channel' to `Early Access Preview 1.4.x` and update.


2. **Install IntelliJ IDEA (or Android Studio) plugin
   The plugin currently supports from 2019.\* to 2020.\*
   It's strongly recommended using the latest IntelliJ or AS, you may update using [JetBrains ToolBox](https://www.jetbrains.com/toolbox-app/)  
   Please note that Eclipse and Visual Studio aren't supported.

   1. Open `File->Settings->Plugins->Marketplace` in your IDE
   2. Search `Kotlin Jvm Blocking Bridge`, download and install
   3. Restart your IDE



2. **Install Gradle plugin.**

`build.gradle` or `build.gradle.kts`
```kotlin
plugins {
  id("net.mamoe.kotlin-jvm-blocking-bridge") version "0.5.0"
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

## Supported compiler backends

Kotlin compiler has two backends, one of which, `JVM`, is the legacy one and is going to be replaced with the new one, named `IR` (Intermediate Representation).  
Currently, legacy `JVM` backend is used by default since it's more stable than the `IR`.

This plugin supports both of them.

**If you meet compilation error when using default JVM backend, please switch to IR backend, otherwise, it's not recommended doing so.**

To switch to IR backend, add into `build.gradle` or `build.gradle.kts`:
```kotlin
tasks.withType<KotlinCompile> {
    kotlinOptions.useIR = true
}
```
