@file:Suppress("RedundantVisibilityModifier", "unused")

package net.mamoe.kjbb

import org.gradle.api.artifacts.dsl.DependencyHandler

public fun DependencyHandler.jvmBlockingBridge(
    version: String? = JBB_VERSION
): Any {
    return "net.mamoe:kotlin-jvm-blocking-bridge:$version"
}