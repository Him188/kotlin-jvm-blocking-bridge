plugins {
    id("org.jetbrains.intellij") version "0.4.16"
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("java")
    signing
    `maven-publish`
    id("com.jfrog.bintray")

    id("com.github.johnrengelman.shadow")
}

repositories {
    maven("http://maven.aliyun.com/nexus/content/groups/public/")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    api(project(":kotlin-jvm-blocking-bridge"))
    api(project(":kotlin-jvm-blocking-bridge-compiler"))

    kapt("com.google.auto.service:auto-service:1.0-rc7")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc7")

    compileOnly("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2020.1.3"
}

tasks.withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask> {
    setChangeNotes("""Update to 2020.1""".trimIndent())
}

setupPublishing(
    groupId = "net.mamoe",
    artifactId = "kotlin-jvm-blocking-bridge-intellij"
)

kotlin {

}

val theProject = project

tasks.getByName("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveClassifier.set("")
    this.dependencyFilter.exclude {
        it.name.contains("intellij", ignoreCase = true) || it.name.contains("idea", ignoreCase = true)
    }
    exclude {
        // exclude ComponentRegistrar which is for CLI compiler.
        it.name == "org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar" && !it.path.contains(theProject.path)
    }
}

tasks.buildPlugin.get().dependsOn(tasks.shadowJar.get())

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