@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "RemoveRedundantBackticks")

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import upload.Bintray
import kotlin.reflect.KProperty

/**
 * Configures the [bintray][com.jfrog.bintray.gradle.BintrayExtension] extension.
 */
@PublishedApi
internal fun Project.`bintray`(configure: com.jfrog.bintray.gradle.BintrayExtension.() -> Unit): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("bintray", configure)

@PublishedApi
internal operator fun <U : Task> RegisteringDomainObjectDelegateProviderWithTypeAndAction<out TaskContainer, U>.provideDelegate(
    receiver: Any?,
    property: KProperty<*>
) = ExistingDomainObjectDelegate.of(
    delegateProvider.register(property.name, type.java, action)
)

@PublishedApi
internal val Project.`sourceSets`: org.gradle.api.tasks.SourceSetContainer
    get() =
        (this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("sourceSets") as org.gradle.api.tasks.SourceSetContainer

@PublishedApi
internal operator fun <T> ExistingDomainObjectDelegate<out T>.getValue(receiver: Any?, property: KProperty<*>): T =
    delegate

/**
 * Configures the [publishing][org.gradle.api.publish.PublishingExtension] extension.
 */
@PublishedApi
internal fun Project.`publishing`(configure: org.gradle.api.publish.PublishingExtension.() -> Unit): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("publishing", configure)

/**
 * Retrieves the [publishing][org.gradle.api.publish.PublishingExtension] extension.
 */
@PublishedApi
internal val Project.`publishing`: org.gradle.api.publish.PublishingExtension
    get() =
        (this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("publishing") as org.gradle.api.publish.PublishingExtension

/**
 * Configures the [signing][org.gradle.plugins.signing.SigningExtension] extension.
 */
@PublishedApi
internal fun Project.`signing`(configure: org.gradle.plugins.signing.SigningExtension.() -> Unit): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("signing", configure)

@Suppress("NOTHING_TO_INLINE")
inline fun Project.setupKotlinSourceSetsSettings() {
    kotlin.runCatching {
        extensions.getByType(KotlinProjectExtension::class.java)
    }.getOrNull()?.run {
        sourceSets.all {

            languageSettings.apply {
                progressiveMode = true

                useExperimentalAnnotation("kotlin.Experimental")
                useExperimentalAnnotation("kotlin.RequiresOptIn")

                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
                useExperimentalAnnotation("kotlin.experimental.ExperimentalTypeInference")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
            }
        }
        sourceSets {
            getByName("test") {
                languageSettings.apply {
                    //languageVersion = "1.4"
                }
            }
        }
    }

    kotlin.runCatching {
        tasks.withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    kotlin.runCatching { tasks.getByName("test", Test::class) }.getOrNull()?.apply {
        useJUnitPlatform()
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Project.setupPublishing(
    groupId: String = project.group.toString(),
    artifactId: String = project.name.toString(),
    bintrayRepo: String = "kotlin-jvm-blocking-bridge",
    bintrayPkgName: String = "kotlin-jvm-blocking-bridge",
    vcs: String = "https://github.com/mamoe/kotlin-jvm-blocking-bridge",
    git: String = "git://github.com/mamoe/kotlin-jvm-blocking-bridge.git",
    overrideFromArtifacts: Any? = null
) {
    tasks.register("ensureBintrayAvailable") {
        doLast {
            if (!Bintray.isBintrayAvailable(project)) {
                error("bintray isn't available. ")
            }
        }
    }

    if (Bintray.isBintrayAvailable(project)) {
        bintray {
            user = Bintray.getUser(project)
            key = Bintray.getKey(project)
            setPublications("mavenJava")
            setConfigurations("archives")

            pkg.apply {
                userOrg = "mamoe"
                repo = bintrayRepo
                name = bintrayPkgName
                setLicenses("Apache-2.0")
                publicDownloadNumbers = true
                vcsUrl = vcs
            }
        }

        @Suppress("DEPRECATION")
        val sourcesJar by tasks.registering(Jar::class) {
            classifier = "sources"
            from(sourceSets["main"].allSource)
        }

        @Suppress("DEPRECATION")
        val javadocJar by tasks.registering(Jar::class) {
            classifier = "javadoc"
        }

        afterEvaluate {
            publishing {
                /*
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url = uri("$buildDir/repo")
                    }
                }*/
                publications {
                    register("mavenJava", MavenPublication::class) {
                        if (overrideFromArtifacts == null) {
                            from(components["java"])
                        } else {
                            artifact(overrideFromArtifacts)
                        }
                        artifact(sourcesJar.get())
                        artifact(javadocJar.get())

                        this.groupId = groupId
                        this.artifactId = artifactId
                        this.version = project.version.toString()

                        pom {
                            name.set(project.name)
                            description.set(project.description)
                            url.set(vcs)

                            licenses {
                                license {
                                    name.set("Apache License 2.0")
                                    url.set("$vcs/blob/master/LICENSE.txt")
                                }
                            }
                            scm {
                                url.set(vcs)
                                connection.set("scm:git:$git")
                            }
                            developers {
                                developer {
                                    name.set("Him188")
                                    url.set("https://github.com/him188")
                                }
                            }
                        }
                    }
                }
            }
        }
    } else println("bintray isn't available. NO PUBLICATIONS WILL BE SET")


    /*
    signing {
        setRequired(provider { gradle.taskGraph.hasTask("publish") })
        sign(publishing.publications)
    }*/
}