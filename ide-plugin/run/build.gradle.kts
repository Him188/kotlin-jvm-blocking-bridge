import net.mamoe.kjbb.compiler.UnitCoercion.COMPATIBILITY
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    id("net.mamoe.kotlin-jvm-blocking-bridge") version "1.10.6"
}

blockingBridge {
//    enableForModule = true

    unitCoercion = COMPATIBILITY
}

group = "me.him188"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }

}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
}