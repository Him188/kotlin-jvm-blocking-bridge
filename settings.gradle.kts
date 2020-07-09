pluginManagement {
    repositories {
        mavenLocal()
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
            }
        }
    }
}

rootProject.name = "kotlin-jvm-blocking-bridge"

include("runtime")
include("compiler-plugin")
include("gradle-plugin")
include("ide-plugin")

fun includeProject(projectPath: String, path: String? = null) {
    include(projectPath)
    if (path != null) project(projectPath).projectDir = file(path)
}