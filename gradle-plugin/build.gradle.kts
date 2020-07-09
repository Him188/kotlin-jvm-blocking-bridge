import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("java")
    signing
    `maven-publish`
    id("com.jfrog.bintray")

    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly(gradleApi())
    implementation(kotlin("gradle-plugin-api"))

    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${Versions.kotlin}")

    api(project(":kotlin-jvm-blocking-bridge-compiler"))
}

setupPublishing(
    groupId = "net.mamoe",
    artifactId = "kotlin-jvm-blocking-bridge-gradle"
)

pluginBundle {
    website = "https://github.com/mamoe/kotlin-jvm-blocking-bridge"
    vcsUrl = "https://github.com/mamoe/kotlin-jvm-blocking-bridge.git"
    tags = listOf("kotlin", "jvm-blocking-bridge")
}

gradlePlugin {
    plugins {
        create("kotlinJvmBlockingBridge") {
            id = "net.mamoe.kotlin-jvm-blocking-bridge"
            displayName = "Kotlin JVM Blocking Bridge"
            description = project.description
            implementationClass = "net.mamoe.kjbb.JvmBlockingBridgeGradlePlugin"
        }
    }
}

tasks.getByName("shadowJar", ShadowJar::class) {
    archiveClassifier.set("")
}

tasks.publishPlugins.get().dependsOn(tasks.shadowJar.get())