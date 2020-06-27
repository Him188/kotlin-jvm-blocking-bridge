@file:Suppress("DuplicatedCode")

package upload

import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate
import java.io.File

object Bintray {

    @JvmStatic
    fun isBintrayAvailable(project: Project): Boolean {
        return kotlin.runCatching {
            getUser(project)
            getKey(project)
        }.isSuccess
    }

    @JvmStatic
    fun getUser(project: Project): String {
        kotlin.runCatching {
            @Suppress("UNUSED_VARIABLE", "LocalVariableName")
            val bintray_user: String by project
            return bintray_user
        }

        kotlin.runCatching {
            @Suppress("UNUSED_VARIABLE", "LocalVariableName")
            val bintray_user: String by project.rootProject
            return bintray_user
        }

        System.getProperty("bintray_user", null)?.let {
            return it.trim()
        }

        File(File(System.getProperty("user.dir")).parent, "/bintray.user.txt").let { local ->
            if (local.exists()) {
                return local.readText().trim()
            }
        }

        File(File(System.getProperty("user.dir")), "/bintray.user.txt").let { local ->
            if (local.exists()) {
                return local.readText().trim()
            }
        }

        error(
            "Cannot find bintray user, " +
                    "please specify by creating a file bintray.user.txt in project dir, " +
                    "or by providing JVM parameter 'bintray_user'"
        )
    }

    @JvmStatic
    fun getKey(project: Project): String {
        kotlin.runCatching {
            @Suppress("UNUSED_VARIABLE", "LocalVariableName")
            val bintray_key: String by project
            return bintray_key
        }

        kotlin.runCatching {
            @Suppress("UNUSED_VARIABLE", "LocalVariableName")
            val bintray_key: String by project.rootProject
            return bintray_key
        }

        System.getProperty("bintray_key", null)?.let {
            return it.trim()
        }

        File(File(System.getProperty("user.dir")).parent, "/bintray.key.txt").let { local ->
            if (local.exists()) {
                return local.readText().trim()
            }
        }

        File(File(System.getProperty("user.dir")), "/bintray.key.txt").let { local ->
            if (local.exists()) {
                return local.readText().trim()
            }
        }

        error(
            "Cannot find bintray key, " +
                    "please specify by creating a file bintray.key.txt in project dir, " +
                    "or by providing JVM parameter 'bintray_key'"
        )
    }

}