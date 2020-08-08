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

#### `UNSUPPORTED_BLOCKING_BRIDGES_IN_INTERFACE`
Reported when `@JvmBlockingBridge` is applied to a function in an interface, and the JVM target is below 8

### Warnings

#### `REDUNDANT_JVM_BLOCKING_BRIDGE_ON_PRIVATE_DECLARATIONS`
Reported when `@JvmBlockingBridge` is applied to a function which is either:
- `private`
- in `private` classes, interfaces, enums or objects.

#### `OVERRIDING_GENERATED_BLOCKING_BRIDGE`
Reported when overriding generated bridges from Java or Kotlin.

#### `IMPLICIT_OVERRIDE_BY_JVM_BLOCKING_BRIDGE`
Reported when bridges are overriding or hiding a super member.
