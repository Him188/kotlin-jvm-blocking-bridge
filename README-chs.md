# kotlin-jvm-blocking-bridge

**[ENGLISH 英文](./README.md)**

为 Kotlin `suspend` 函数快速生成‘阻塞式方法桥’的 Kotlin 编译器插件。

## 截图

<details>

<summary>点击左侧箭头查看</summary>

Kotlin 挂起函数:  
![image_2.png](https://i.loli.net/2020/08/08/d5cYwhQqeuj8Nvf.png)

阻塞式方法桥调用:  
![image.png](https://i.loli.net/2020/08/08/tJyGeOcB8E4muQ5.png)

文档和跳转支持:  
![image_1.png](https://i.loli.net/2020/08/08/koCl6zj4OAJ5aUN.png)

</details>

## 动机

Kotlin `suspend` 函数编译后会被加上一个额外参数 `$completion: Continuation`。在 Java 调用这样的方法很困难，我们可以做一些兼容:

```kotlin
suspend fun downloadImage(): Image
```

我们可以添加一个‘阻塞式方法桥’：

```kotlin
@JvmName("downloadImage") // 避免引用歧义
fun downloadImageBlocking(): Image = runBlocking { downloadImage() }
```

这样，Java 使用者可以直接通过 `downloadImage()` 调用，就像在 Kotlin 调用 `suspend` 函数。即使这损失了一些性能，但通常我们不在乎它。

然而，这也带来了一些问题:

- KDoc 需要从原函数复制到额外添加的函数，并且修改时要同时修改两者
- 修改函数签名（参数，修饰符，返回值，注解）变得十分不便
- 我们不希望将 `downloadImageBlocking` 暴露给 Kotlin 调用方，但没有方法隐藏它们  
  一个不太好的解决方法是添加 `RequiresOptIn`：
  ```kotlin
  @RequiresOptIn(level = ERROR)
  annotation class JavaFriendlyApi
  
  @JavaFriendlyApi // 于是当 Kotlin 调用者调用这个函数时，IDE 会报告 ERROR 级别的错误。尽管 Kotlin 仍然能看到这些方法。
  @JvmName("downloadImage") // 避免引用歧义
  fun downloadImageBlocking(): Image = runBlocking { downloadImage() }
  ```
  但这似乎不是最好的----我们要重复 `JavaFriendlyApi` 这样的注解很多次，还需要到处使用它。

**本编译器插件设计为最小化这样兼容 Java 时所作的额外工作----仅需添加一个注解**:

```kotlin
@JvmBlockingBridge
suspend fun downloadImage(): Image
```

编译器会帮助生成上述的‘阻塞式方法桥’ `fun downloadImage()`，使用‘相同’的方法签名（编译级的生成不会引起调用歧义），且更高效。

### Examples Of Usages

1. 提供在 Java 调用 `suspend` 函数的最简单方式:
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

2. 在测试中，使用 `@JvmBlockingBridge` 来运行 `suspend` 的测试函数而不需要 `runBlocking`：

   ```kotlin
   @file:JvmBlockingBridge
   
   class SomeTests {
       @Test
       suspend fun test() { /* ... */ }
   }
   ```

## 稳定性

编译器插件有超过 150 个单元测试来确保每一项功能的正常运行。

拥有 10 万行 Kotlin 代码的库 [mirai](https://github.com/mamoe/mirai) 大量地在各种情况下使用了这个编译器插件。mirai 拥有严格二进制兼容测试，正在被成千上万的用户使用。  
这意味着 Kotlin Jvm Blocking Bridge 提供稳定的编译结果，而且适用于生产环境。

## 使用要求

- Gradle（仅在 6.0+ 环境通过测试）
- **Kotlin `1.4.20` 或更高**
- IntelliJ IDEA 或 Android Studio（推荐保持新稳定版本）

## 现在体验

### 使用者

**如果一个库使用了 Kotlin JVM Blocking Bridge，依赖方无需特别在意，可以就像普通库一样添加编译依赖使用。** 即依赖使用或不使用 KJBB 编译的库的流程都是一样的。

### 库和应用程序作者

如果你正开发一个库或应用程序，你需要安装 Gradle 插件来获取编译支持，和安装 IntelliJ IDEA 插件来获取编辑支持。

#### **安装 IntelliJ IDEA (或 Android Studio) 插件**

本插件支持 IntelliJ IDEA 2020.\* 和 2021.\*。  
Eclipse 和 Visual Studio 或其他 IDE 均不受支持。

一键安装：<https://plugins.jetbrains.com/embeddable/install/14816>，或者也可以手动安装：

1. 打开 `File->Settings->Plugins->Marketplace`
2. 搜索 `Kotlin Jvm Blocking Bridge`，下载并安装
3. 重启 IDE

#### **安装 Gradle 插件.**

`build.gradle.kts`

```kotlin
plugins {
    id("me.him188.kotlin-jvm-blocking-bridge") version "1.11.0"
}
```

可在 [releases](https://github.com/Him188/kotlin-jvm-blocking-bridge/releases) 获取 `VERSION`, 如 `2.0.0-160.3`.

本插件会自动添加如下的运行时依赖:

```kotlin
implementation("me.him188:kotlin-jvm-blocking-bridge:VERSION")
```

因此只需要安装插件，而不需要添加依赖即可使用。请确保在运行时有这个依赖（通常不需要做额外工作）。


> 如果 Gradle 无法下载这个插件，请在 `settings.gradle` 或 `settings.gradle.kts` 中添加 `gradlePluginPortal()`:
> ```kotlin
> pluginManagement {
>     repositories {
>         gradlePluginPortal()
>     }
> }
> ```

## 支持的编译器后端

Kotlin 拥有两个编译器后端，旧 `JVM` 和新 `IR`(Internal Representation)。  
Kotlin 默认使用 `JVM` 后端，但即将在 Kotlin 1.5 启用 `IR` 后端。

本插件同时支持这两个后端。在两个后端产生的编译结果都是相同的。

## 模块

如果你感兴趣于这个项目的原理，本章节可能会有帮助。当然你也可以直接使用插件了。

- **运行时库**  *提供 @JvmBlockingBridge 注解*
- **编译器插件**  *提供编译代码生成，编译目标为 JVM 字节码或 Kotlin IR*
- **ide-plugin**

**在 [BridgeFeatures.md](BridgeFeatures.md) 阅读规范**

### 运行时库

- 提供 `public annotation class me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge`
- 提供 `internal annotation class me.him188.kotlin.jvm.blocking.bridge.GeneratedBlockingBridge`，由编译器插件自动添加到生成的方法桥上.
- 提供编译后的阻塞式方法桥需要调用的一些库函数.

### 编译器插件

对于 Kotlin `suspend` 函数:

```kotlin
@JvmBlockingBridge
suspend fun test(a1: Int, a2: Any): String
```

本编译器插件生成与原函数具有‘相同’签名的‘阻塞式方法桥’ (仅 Java 可见)

```kotlin
@GeneratedBlockingBridge
fun test(a1: Int, a2: Any): String = `$runSuspend$` {
        test(a1, a2) // 调用原 suspend `test`  
    } // `$runSuspend$` 是一个运行时库中的函数, 因此不依赖 kotlinx-coroutines-core.
```

### IDE (IntelliJ) 插件

- 让 Java 用户能引用阻塞式方法桥 (即使它们还没有生成)
- 为 Kotlin 用户隐藏生成的阻塞式方法桥 (即使它们已经生成)
