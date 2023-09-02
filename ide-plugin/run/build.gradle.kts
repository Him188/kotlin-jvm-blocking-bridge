import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.20"
    id("me.him188.kotlin-jvm-blocking-bridge") version "3.0.0-180.1"
}

blockingBridge {
//    enableForModule = true
}

group = "me.him188"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }

}

dependencies {
    api("me.him188:kotlin-jvm-blocking-bridge-runtime:3.0.0-180.1")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
}