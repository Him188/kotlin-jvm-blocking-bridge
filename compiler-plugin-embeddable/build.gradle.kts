import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("me.him188.maven-central-publish")
    kotlin("jvm")
    kotlin("plugin.serialization")
    signing
    id("com.github.johnrengelman.shadow")
}

kotlin.targets.asSequence()
    .flatMap { it.compilations }
    .filter { it.platformType == org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm }
    .map { it.kotlinOptions }
    .filterIsInstance<org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions>()
    .forEach { it.jvmTarget = "1.8" }

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
