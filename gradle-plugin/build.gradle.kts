plugins {
    id("io.github.karlatemp.publication-sign")
    kotlin("jvm")
    kotlin("kapt")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("java")
    signing
    `maven-publish`

    id("com.github.johnrengelman.shadow")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(gradleApi())
    compileOnly(kotlin("gradle-plugin-api"))
    compileOnly(kotlin("gradle-plugin"))
    //implementation("io.github.classgraph:classgraph:4.8.47")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${Versions.kotlin}")

    //kapt("com.google.auto.service:auto-service:1.0-rc7")
    //compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc7")

    api(project(":kotlin-jvm-blocking-bridge-compiler"))
}

//embeddableCompiler()

//setupPublishing(
//    groupId = "net.mamoe",
//    artifactId = "kotlin-jvm-blocking-bridge-gradle",
//    overrideFromArtifacts = tasks.getByName("embeddable") as ShadowJar
//)

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

/*
tasks.getByName("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveClassifier.set("")
}

tasks.publishPlugins.get().dependsOn(tasks.shadowJar.get())*/