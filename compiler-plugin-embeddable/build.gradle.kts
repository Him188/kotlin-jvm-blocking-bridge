import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("java")
    signing
    `maven-publish`
    id("com.jfrog.bintray")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    api(project(":kotlin-jvm-blocking-bridge-compiler"))
}

embeddableCompiler()

setupPublishing(
    groupId = "net.mamoe",
    artifactId = "kotlin-jvm-blocking-bridge-compiler-embeddable",
    overrideFromArtifacts = tasks.getByName("embeddable") as ShadowJar
)