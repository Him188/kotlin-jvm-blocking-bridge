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
**Available only when targeting Java 8 or higher**

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
