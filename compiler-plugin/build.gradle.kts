plugins {
    id("net.mamoe.maven-central-publish")
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("java")
    id("com.github.johnrengelman.shadow")
}

dependencies includeInShadow@{
    implementation(project(":kotlin-jvm-blocking-bridge-runtime"))
}

dependencies compileOnly@{
    compileOnly(kotlin("stdlib")) // don't include stdlib in shadow
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}")
    kapt("com.google.auto.service:auto-service:1.0")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0")
}

dependencies tests@{
    testImplementation(project(":kotlin-jvm-blocking-bridge-runtime"))

    testImplementation(kotlin("reflect"))

    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${Versions.kotlin}")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}") // for debugger
    //testImplementation("org.jetbrains.kotlin:kotlin-compiler:${Versions.kotlin}")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")

    testImplementation(kotlin("test-junit5"))

    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.5")
    testImplementation("org.assertj:assertj-core:3.21.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

embeddableCompiler()

val test by tasks.getting(Test::class) {
    dependsOn(tasks.getByName("embeddable"))
    this.classpath += tasks.getByName("embeddable").outputs.files
    this.classpath =
        files(*this.classpath.filterNot {
            it.absolutePath.replace("\\", "/").removeSuffix("/").endsWith(("build/classes/kotlin/main"))
        }.toTypedArray())
}

mavenCentralPublish {
    packageGroup = Versions.publicationGroup
    singleDevGithubProject("Him188", "kotlin-jvm-blocking-bridge")
    licenseApacheV2()
}