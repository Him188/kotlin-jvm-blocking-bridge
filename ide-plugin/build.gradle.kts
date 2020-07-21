plugins {
    //id("org.jetbrains.intellij") version "0.4.16"
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("java")
    signing
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation(project(":kotlin-jvm-blocking-bridge"))
    implementation(project(":kotlin-jvm-blocking-bridge"))

}

/*
// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2020.1"
}

tasks.withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask>() {
    setChangeNotes("""Update to 2020.1""".trimIndent())
}*/

setupPublishing(
    groupId = "net.mamoe",
    artifactId = "kotlin-jvm-blocking-bridge-ide"
)

kotlin {

}

/*
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}*/

/*
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}*/