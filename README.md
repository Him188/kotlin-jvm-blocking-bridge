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

## Motivation
Kotlin suspend function is compiled with an additional `$completion: Continuation` parameter, making it hard to call from Java. To help integration with Java, we may make extra effort to simplify calling:
```kotlin
suspend fun downloadImage(): Image
```
We can add
```kotlin
@JvmName("downloadImage") // avoid resolution ambiguity
fun downloadImageBlocking(): Image = runBlocking { downloadImage() }
```
so Java users can also call `downloadImage()` just like calling the `suspend` function, without implementing a `Continuation`.

However, there several problems:
- KDoc is copied to the bridge, when updating, copying is also required.
- Changing the signature becomes inconvenient.
- `downloadImageBlocking` is also exposed to Kotlin callers, and we can't hide them. We can make it 'difficult' to call by adding `RequiresOptIn`
  ```kotlin
  @RequiresOptIn(level = ERROR)
  annotation class JavaFriendlyApi
  
  @JavaFriendlyApi // so IDE reports 'Experimental API usage' error for calling from Kotlin.
  @JvmName("downloadImage") // avoid resolution ambiguity
  fun downloadImageBlocking(): Image = runBlocking { downloadImage() }
  ```


This plugin has been designed to minimize work against Java compatibility, to provide the ability to call Kotlin's `suspend` function in a 'blocking' way:
```kotlin
@JvmBlockingBridge
suspend fun downloadImage(): Image
```

The Kotlin JVM Blocking Bridge compiler will generate such blocking bridges automatically.

### Examples Of Usages

1. Provide the easiest way to call `suspend` functions from Java:
   ```kotlin
   interface Image
   
   object ImageManager {
       @JvmStatic
       @JvmBlockingBridge
       suspend fun getImage(): Image
   }
   ```
   ```java
   class Test {
       public static void main(String[] args){
           Image image = ImageManager.getImage(); // just like in Kotlin, no need to implement Continuation.
       }
   }
   ```

2. In tests, add `@JvmBlockingBridge` to run suspend tests without `runBlocking`:

   ```kotlin
   @file:JvmBlockingBridge
   
   class SomeTests {
       @Test
       suspend fun test() { /* ... */ }
   }
   ```

## Stability
There are more than 150 unit tests ensuring the functioning of this plugin.

This compiler plugin has been used all over the library [mirai](https://github.com/mamoe/mirai), which consists of 100k lines of code, covers all the circumstances you may use this plugin for, and has been used by thousand of customers.  
This means that Kotlin Jvm Blocking Bridge produces high stability and is capable for production use.


## Requirements
- Gradle (`6.0` or higher recommended)
- **Kotlin `1.4.20` or higher**
- IntelliJ IDEA or Android Studio (newer version recommended)

## Try now

The plugin is ready to use.

### Library users

There is no special requirements for library users. They can use any IDEs and any build tools.

### Library and application authors

If you're developing a library, or an application using both Java and Kotlin with KJBB, or anything that relies on source code analysis, please install the Gradle plugin **and** the IntelliJ plugin.

#### **Install IntelliJ IDEA (or Android Studio) plugin**
   The plugin supports 2020.\* and 2021.\*  
   It's strongly recommended using the latest IJ or AS, you may update using [JetBrains ToolBox](https://www.jetbrains.com/toolbox-app/)  
   Please note that Eclipse and Visual Studio aren't supported.

   One-key install: [Install to IntelliJ IDEA](https://plugins.jetbrains.com/embeddable/install/14816)

   You can also install manually:

   1. Open `File->Settings->Plugins->Marketplace` in your IDE
   2. Search `Kotlin Jvm Blocking Bridge`, download and install
   3. Restart your IDE

#### **Install Gradle plugin**

`build.gradle` or `build.gradle.kts`
```kotlin
plugins {
  id("net.mamoe.kotlin-jvm-blocking-bridge") version "1.10.5"
}
```

Then you're done. You can then use the `@JvmBlockingBridge` annotation.

The plugin will automatically install the runtime dependency, like:
```kotlin
implementation("net.mamoe:kotlin-jvm-blocking-bridge:1.10.5")
```
Please make sure you have it in application runtime (usually you don't need to do anything about it).

## Supported compiler backends

Kotlin compiler has two backends, one of which, `JVM`, is the legacy one and is going to be replaced with the new one, named `IR` (Internal Representation).  
Currently, legacy `JVM` backend is used by default since it's more stable than the `IR`.

This plugin supports both of them.

`IR` is faster and is going to be stable in Kotlin 1.5 release.

To switch to IR backend, add into `build.gradle` or `build.gradle.kts`:
```kotlin
tasks.withType<KotlinCompile> {
    kotlinOptions.useIR = true
}
```


## Kotlin mulitplatform projects (MPP)

KJBB compiler and gradle plugins supports Kotlin MPP 'as is'. Bridges are generated only for JVM and Android JVM targets. On other platforms, the `@JvmBlockingBridge` is also available but does nothing to the compilation.


## Modules

This chapter lists the modules in the project. If you are interested in the internal mechanism of this plugin, it may be helpful.

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
fun test(a1: Int, a2: Any): String = `$runSuspend$` { 
    test(a1, a2) // calls the original suspend `test` 
} // `$runSuspend$` is a internal function in the runtime library, so we doesn't require kotlinx-coroutines-core. 
```

### IDE plugin

- Adds compiler-plugin dependency inspection when using `@JvmBlockingBridge`
- Add reference resolution for generated bridge functions for Java
