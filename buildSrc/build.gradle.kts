plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    sourceSets.all {
        languageSettings.optIn("kotlin.Experimental")
        languageSettings.optIn("kotlin.RequiresOptIn")
    }
}

dependencies {
    fun kotlinx(id: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$id:$version"
    fun ktor(id: String, version: String) = "io.ktor:ktor-$id:$version"

    compileOnly(gradleApi())
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:${version("kotlin")}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.0")

    compileOnly("com.github.jengelman.gradle.plugins:shadow:6.0.0")
}

fun version(name: String): String = project.rootDir.resolve("src/main/kotlin/Versions.kt").readText()
    .substringAfter("$name = \"", "")
    .substringBefore("\"", "")
    .also {
        check(it.isNotBlank()) { "Cannot find version $name" }
    }