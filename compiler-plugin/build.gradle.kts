import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("me.him188.maven-central-publish")
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

kotlin.targets.asSequence()
    .flatMap { it.compilations }
    .filter { it.platformType == KotlinPlatformType.jvm }
    .map { it.kotlinOptions }
    .filterIsInstance<KotlinJvmOptions>()
    .forEach { it.jvmTarget = "1.8" }

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies includeInShadow@{
    implementation(project(":kotlin-jvm-blocking-bridge-runtime"))
}

dependencies compileOnly@{
    compileOnly(kotlin("stdlib")) // don't include stdlib in shadow
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}")
    kapt("com.google.auto.service:auto-service:1.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")
}

dependencies tests@{
    testImplementation(project(":kotlin-jvm-blocking-bridge-runtime"))

    testImplementation(kotlin("reflect"))

    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${Versions.kotlin}")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}") // for debugger
    //testImplementation("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")

    testImplementation(kotlin("test-junit5"))

    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0") {
        exclude("org.jetbrains.kotlin", "kotlin-annotation-processing-embeddable")
    }
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:${Versions.kotlin}")


    testImplementation("org.assertj:assertj-core:3.22.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

embeddableCompiler()

val test by tasks.getting(Test::class) {
    dependsOn(tasks.getByName("embeddable"))
    afterEvaluate {
        classpath = tasks.getByName("embeddable").outputs.files + classpath
        classpath =
            files(*classpath.filterNot {
                it.absolutePath.replace("\\", "/").removeSuffix("/").endsWith("build/classes/kotlin/main")
                        || it.absolutePath.replace("\\", "/").removeSuffix("/").endsWith("build/classes/java/main")
            }.toTypedArray())
    }
}

mavenCentralPublish {
    this.workingDir = rootProject.buildDir.resolve("temp/pub/").apply { mkdirs() }
    useCentralS01()
    singleDevGithubProject("Him188", "kotlin-jvm-blocking-bridge")
    licenseApacheV2()
}