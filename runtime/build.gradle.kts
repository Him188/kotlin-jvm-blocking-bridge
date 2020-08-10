@file:Suppress("UNUSED_VARIABLE")

plugins {
    kotlin("multiplatform")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("java")
    `maven-publish`
    signing
    // id("com.bmuschko.nexus")
    //  id("io.codearte.nexus-staging")
}

kotlin {
    explicitApi()

    targets {
        jvm()
        js {
            useCommonJs()
        }
        val hostOs = System.getProperty("os.name")
        val isMingwX64 = hostOs.startsWith("Windows")
        val nativeTarget = when {
            hostOs == "Mac OS X" -> macosX64("native")
            hostOs == "Linux" -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                compileOnly(kotlin("stdlib-common")) // stdlib should be excluded from embeddable ShadowJar
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                compileOnly(kotlin("stdlib")) // stdlib should be excluded from embeddable ShadowJar
                //runtimeOnly(kotlin("stdlib"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("test"))
                implementation(kotlin("test-junit5"))

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
/*
setupPublishing(
    groupId = "net.mamoe",
    artifactId = "kotlin-jvm-blocking-bridge"
)
*/
apply(from = "gradle/publish.gradle")

/*
nexus {
    sign = true
    repositoryUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
    snapshotRepositoryUrl = "https://oss.sonatype.org/content/repositories/snapshots/"
}

nexusStaging {
    packageGroup = "net.mamoe" //optional if packageGroup == project.getGroup()
    // stagingProfileId = "yourStagingProfileId" //when not defined will be got from server using "packageGroup"
}

extraArchive {
    sources = false
    tests = false
    javadoc = false
}*/
