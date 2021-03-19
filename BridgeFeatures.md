# Bridge Features
This file describes the behavior that compiler shows when dealing with `@JvmBlockingBridge`.

## Bridges generating
For functions that are capable to have blocking bridges (with the following strategies),
the Kotlin Blocking Bridge compiler plugin will generate an additional function, with the same method signature,
but without `suspend` modifier, to make it callable from Java.

### Bridges in `final` classes
Features of generated bridges:
- `final` always
- Same visibility with the original function

**Note**: All the bridges in a `final` class are final.

### Bridges in `open`, `sealed` or `abstract` classes

Features of generated bridges:
- `open` always
- Same visibility with the original function

#### Function inheritance
If a superclass have bridge functions, blocking bridges will **not** generate in child classes unless explicitly specified `@JvmBlockingBridge`.

Example:
```kotlin
abstract class A {
    @JvmBlockingBridge
    abstract suspend fun test()
    @JvmBlockingBridge
    abstract suspend fun test2()
}
class B : A() {
    @JvmBlockingBridge
    override suspend fun test()
    override suspend fun test2()
}
```
Compilation result:
```kotlin
abstract class A {
    @JvmBlockingBridge
    abstract suspend fun test()
    @JvmBlockingBridge
    abstract suspend fun test2()
    
    @GeneratedBlockingBridge
    open fun test()
    @GeneratedBlockingBridge
    open fun test2()
}
class B : A() {
    @JvmBlockingBridge
    override suspend fun test()
    override suspend fun test2()
    
    @GeneratedBlockingBridge
    open override fun test() // because `test` declared in `B` has explicit `@JvmBlockingBridge`
    // `test2` absent, because `test2` in `B` doesn't have explicit `@JvmBlockingBridge`
}
```

### Bridges in interfaces
**Available only when targeting JVM 8 or higher**

Features of generated bridges:
- `open` always
- With `default` implementations
- Same visibility with the original function
- Following inheritance rules with [Function inheritance](#function-inheritance)

Example:
```kotlin
interface A {
    @JvmBlockingBridge
    suspend fun test()
    @JvmBlockingBridge
    suspend fun test2()
}
class B : A() {
    @JvmBlockingBridge
    override suspend fun test()
    override suspend fun test2()
}
```
Compilation result:
```kotlin
interface A {
    @JvmBlockingBridge
    suspend fun test()
    @JvmBlockingBridge
    suspend fun test2()
    
    @GeneratedBlockingBridge
    fun test() { /* call suspend test() */ } // 'default' implementation, which is supported by Java 8+
    @GeneratedBlockingBridge
    fun test2() { /* call suspend test2() */ } // 'default' implementation, which is supported by Java 8+
}
class B : A() {
    @JvmBlockingBridge
    override suspend fun test()
    override suspend fun test2()
    
    @GeneratedBlockingBridge
    open override fun test() // because `test` declared in `B` has explicit `@JvmBlockingBridge`
    // `test2` absent, because `test2` in `B` doesn't have explicit `@JvmBlockingBridge`
}
```

### Stdlib annotations

When function is annotated with `@JvmSynthetic`, no blocking bridge will be generated for it.

`@JvmOverloads` and `@JvmName` is useful to blocking bridges.


### `@JvmBlockingBridge` on classes

When `@JvmBlockingBridge` is applied on classes, all the `suspend` functions will be checked about capability of having blocking bridges, which is:
- effectively public: `public`, `protected` or `@PublishedApi`
- not `@JvmSynthetic`

```kotlin
@JvmBlockingBridge
class A {
    suspend fun test()
}
```

Compilation result:
```kotlin
@JvmBlockingBridge
class A {
    @GeneratedBlockingBridge
    fun test()
}
```

IntelliJ plugin will add a inlay hint of `@JvmBlcokingBridge` onto the functions that is going to have blocking bridges generated.


### Enable for module

When enabled in `build.gradle` by 
```kotlin
blockingBridge {
    enableForModule = true
}
```
, **all** `susupend` functions will be compiled with a blocking bridge if possible. 
This is practically like adding `@JvmBlockingBridge` to every `suspend` functions and suppress errors if not applicable.

In other words, if function has the following characteristics, they do not have blocking bridges. Otherwise whey do.
- is not `suspend`
- is local
- is `private` or inside `private` or `internal` class
- is `internal` without `@PublishedApi`
- has `@JvmSynthetic` (invisible from Java)
- is top-level but not using IR compiler
- uses inline classes as parameter or return type but not using IR compiler
- is inside interface but JVM target is below 8 (default implementation in interface not supported)


IntelliJ plugin will show you a inlay hint on the capable functions that may be 'inferred' a `@JvmBlcokingBridge`.

## Compiler options

|       Name        | Values                  | Description                                                                                |
|:-----------------:|:------------------------|:-------------------------------------------------------------------------------------------|
|  `unit-coercion`  | `VOID`, `COMPATIBILITY` | Strategy on mapping from `Unit` to `void` in JVM backend. `VOID` by default (recommended). |
| `enableForModule` | `true`, `false`         | Generate blocking bridges for all suspend functions in the module where possible.          |


## [Inspections](compiler-plugin/src/main/kotlin/net/mamoe/kjbb/compiler/diagnostic/BlockingBridgeErrorsRendering.kt#L8)

### Errors

#### `INAPPLICABLE_JVM_BLOCKING_BRIDGE`
Reported when `@JvmBlockingBridge` is applied to a function which is either:
- not `suspend`
- declared in Java class

#### `INLINE_CLASSES_NOT_SUPPORTED`
Reported when `@JvmBlockingBridge` is applied to a function which either:
- is in `inline` class
- has parameter types or return type that is `inline` classes

#### `INTERFACE_NOT_SUPPORTED`
Reported when `@JvmBlockingBridge` is applied to a function in an interface, and the JVM target is below 8

#### `TOP_LEVEL_FUNCTIONS_NOT_SUPPORTED`
*(Since `1.1.0`)*
Reported using `@JvmBlockingBridge` on top-level functions (as there isn't a way to hack into the file-class codegen).

**Note**:
Codegen extensions for top-level functions are not available in the old JVM backend. The compiler plugin generate bridges for top-level functions only if using IR backend, otherwise, an error `TOP_LEVEL_FUNCTIONS_NOT_SUPPORTED` will be reported.

However, the IDE plugin cannot decide which compiler backend is currently in use, so the error `TOP_LEVEL_FUNCTIONS_NOT_SUPPORTED` is always reported by the IDE.

If you use IR backend, you can suppress this error by adding `@Suppress("TOP_LEVEL_FUNCTIONS_NOT_SUPPORTED")`.  
If you use JVM backend, you may change the way designing your API.

### Warnings

#### `REDUNDANT_JVM_BLOCKING_BRIDGE_ON_PRIVATE_DECLARATIONS`
Reported when `@JvmBlockingBridge` is applied to a function which is either:
- `private`
- in `private` classes, interfaces, enums or objects.

#### `OVERRIDING_GENERATED_BLOCKING_BRIDGE`
*(TODO)*  
Reported when overriding generated bridges from Java or Kotlin.
