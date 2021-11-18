/**
 * Some of the code is copied from JetBrains/Kotlin. Copyright JetBrains s.r.o.
 */


import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File

const val kotlinEmbeddableRootPackage = "org.jetbrains.kotlin"

val packagesToRelocate =
    listOf(
        "com.intellij",
        "com.google",
        "com.sampullara",
        "org.apache",
        "org.jdom",
        "org.picocontainer",
        "org.jline",
        "org.fusesource",
        "net.jpountz",
        "one.util.streamex",
        "it.unimi.dsi.fastutil",
        "kotlinx.collections.immutable"
    )

// The shaded compiler "dummy" is used to rewrite dependencies in projects that are used with the embeddable compiler
// on the runtime and use some shaded dependencies from the compiler
// To speed-up rewriting process we want to have this dummy as small as possible.
// But due to the shadow plugin bug (https://github.com/johnrengelman/shadow/issues/262) it is not possible to use
// packagesToRelocate list to for the include list. Therefore the exclude list has to be created.
val packagesToExcludeFromDummy =
    listOf(
        "org/jetbrains/kotlin/**",
        "org/intellij/lang/annotations/**",
        "org/jetbrains/jps/**",
        "META-INF/**",
        "com/sun/jna/**",
        "com/thoughtworks/xstream/**",
        "javaslang/**",
        "*.proto",
        "messages/**",
        "net/sf/cglib/**",
        "one/util/streamex/**",
        "org/iq80/snappy/**",
        "org/jline/**",
        "org/xmlpull/**",
        "*.txt"
    )

@Suppress("NOTHING_TO_INLINE") // shadow version differs
@PublishedApi
internal inline fun ShadowJar.configureEmbeddableCompilerRelocation(withJavaxInject: Boolean = true) {
    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf")
    packagesToRelocate.forEach {
        relocate(it, "$kotlinEmbeddableRootPackage.$it")
    }
    if (withJavaxInject) {
        relocate("javax.inject", "$kotlinEmbeddableRootPackage.javax.inject")
    }
    relocate("org.fusesource", "$kotlinEmbeddableRootPackage.org.fusesource") {
        // TODO: remove "it." after #KT-12848 get addressed
        exclude("org.fusesource.jansi.internal.CLibrary")
    }
}

@PublishedApi
internal inline fun Project.compilerShadowJar(
    taskName: String,
    crossinline body: ShadowJar.() -> Unit
): TaskProvider<out ShadowJar> {

    // val compilerJar = configurations.getOrCreate("compilerJar")
    //dependencies.add(compilerJar.name, dependencies.project(":kotlin-compiler", configuration = "runtimeJar"))

    return tasks.register<ShadowJar>(taskName) {
        @Suppress("DEPRECATION")
        destinationDir = File(buildDir, "libs")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from((tasks.getByName("shadowJar") as ShadowJar).configurations)
        from((tasks.getByName("shadowJar") as ShadowJar).source)
        body()
    }
}

fun ConfigurationContainer.getOrCreate(name: String): Configuration = findByName(name) ?: create(name)

inline fun Project.embeddableCompiler(
    taskName: String = "embeddable",
    crossinline body: ShadowJar.() -> Unit = {}
): TaskProvider<out ShadowJar> {
    return compilerShadowJar(taskName) {
        group = "shadow"
        configureEmbeddableCompilerRelocation()
        body()
    }
}
