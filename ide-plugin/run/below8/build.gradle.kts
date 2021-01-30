import net.mamoe.kjbb.compiler.UnitCoercion.COMPATIBILITY
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("net.mamoe.kotlin-jvm-blocking-bridge")
}

blockingBridge {
    unitCoercion = COMPATIBILITY
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.6"
}