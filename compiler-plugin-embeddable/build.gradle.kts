import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("me.him188.maven-central-publish")
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("java")
    signing
    id("com.github.johnrengelman.shadow")
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
    sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    api(project(":kotlin-jvm-blocking-bridge-compiler"))
}

embeddableCompiler()

mavenCentralPublish {
    this.workingDir = rootProject.buildDir.resolve("temp/pub/").apply { mkdirs() }
    useCentralS01()
    singleDevGithubProject("Him188", "kotlin-jvm-blocking-bridge")
    licenseApacheV2()

    addProjectComponents = false
    publication {
        artifact(tasks.getByName("embeddable") as ShadowJar)
    }
}