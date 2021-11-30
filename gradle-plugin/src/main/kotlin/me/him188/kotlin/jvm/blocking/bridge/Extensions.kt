@file:Suppress("RedundantVisibilityModifier", "unused")

package me.him188.kotlin.jvm.blocking.bridge

import org.gradle.api.artifacts.dsl.DependencyHandler

public fun DependencyHandler.jvmBlockingBridge(
    version: String? = KJBB_VERSION,
): Any {
    return "net.mamoe:kotlin-jvm-blocking-bridge:$version"
}