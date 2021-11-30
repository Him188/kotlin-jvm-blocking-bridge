import me.him188.kotlin.jvm.blocking.bridge.compiler.UnitCoercion.COMPATIBILITY
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("me.him188.kotlin-jvm-blocking-bridge")
}

blockingBridge {
    unitCoercion = COMPATIBILITY
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.6"
}