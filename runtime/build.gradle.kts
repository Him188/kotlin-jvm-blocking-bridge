@file:Suppress("UNUSED_VARIABLE")

plugins {
    id("net.mamoe.maven-central-publish")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    explicitApi()

    targets {
        jvm()
        js {
            useCommonJs()
        }
        apply(from = file("gradle/compile-native-multiplatform.gradle"))

    }

    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }

        val commonTest by getting {
            dependencies {
            }
        }

        val jvmMain by getting {
            dependencies {
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:5.2.0")
                implementation("org.junit.jupiter:junit-jupiter-engine:5.2.0")
            }
        }

        val jsMain by getting {
            dependencies {
                compileOnly(kotlin("stdlib-js"))
            }
        }
        val nativeMain by getting {
            dependencies {
            }
        }
    }
}

mavenCentralPublish {
    artifactId = "kotlin-jvm-blocking-bridge-runtime"
    packageGroup = Versions.publicationGroup
    singleDevGithubProject("Him188", "kotlin-jvm-blocking-bridge")
    licenseApacheV2()

    publishPlatformArtifactsInRootModule = "jvm"
}