pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        jcenter()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-dev")
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            val version = requested.version
            when (requested.id.id) {
                "org.jetbrains.kotlin.jvm" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${version}")
                "org.jetbrains.kotlin.kapt" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${version}")
                "org.jetbrains.kotlin.plugin.serialization" -> useModule("org.jetbrains.kotlin:kotlin-serialization:${version}")
                "com.jfrog.bintray" -> useModule("com.jfrog.bintray.gradle:gradle-bintray-plugin:$version")
                "com.gradle.plugin-publish" -> useModule("com.gradle.publish:plugin-publish-plugin:$version")
            }
        }
    }
}

rootProject.name = "kotlin-jvm-blocking-bridge"

includeProject("kotlin-jvm-blocking-bridge", "runtime")
includeProject("kotlin-jvm-blocking-bridge-compiler", "compiler-plugin")
includeProject("kotlin-jvm-blocking-bridge-gradle", "gradle-plugin")
includeProject("kotlin-jvm-blocking-bridge-intellij", "ide-plugin")

fun includeProject(name: String, path: String) {
    include(path)
    project(":$path").name = name
}