pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        mavenLocal()
    }
}

rootProject.name = "kotlin-jvm-blocking-bridge"

includeProject("kotlin-jvm-blocking-bridge-runtime", "runtime")
includeProject("kotlin-jvm-blocking-bridge-compiler", "compiler-plugin")
includeProject("kotlin-jvm-blocking-bridge-compiler-embeddable", "compiler-plugin-embeddable")
includeProject("kotlin-jvm-blocking-bridge-gradle", "gradle-plugin")
includeProject("kotlin-jvm-blocking-bridge-intellij", "ide-plugin")

fun includeProject(name: String, path: String) {
    include(path)
    project(":$path").name = name
}