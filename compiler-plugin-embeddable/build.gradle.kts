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

dependencies {
    api(project(":kotlin-jvm-blocking-bridge-compiler"))
}

embeddableCompiler()

mavenCentralPublish {
    packageGroup = Versions.publicationGroup
    singleDevGithubProject("Him188", "kotlin-jvm-blocking-bridge")
    licenseApacheV2()

    addProjectComponents = false
    publication {
        artifact(tasks.getByName("embeddable") as ShadowJar)
    }
}