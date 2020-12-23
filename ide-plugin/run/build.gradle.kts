import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30-M1"
    // id("net.mamoe.kotlin-jvm-blocking-bridge") version "1.5.0-KT-1.4.30-M1"
}

group = "me.him188"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
}

dependencies {
    api("net.mamoe:kotlin-jvm-blocking-bridge:1.5.0-KT-1.4.30-M1")
    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}