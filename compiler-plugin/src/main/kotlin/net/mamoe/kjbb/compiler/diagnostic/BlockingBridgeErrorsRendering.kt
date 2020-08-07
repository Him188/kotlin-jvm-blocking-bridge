package net.mamoe.kjbb.compiler.diagnostic

import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeErrors.INAPPLICABLE_JVM_BLOCKING_BRIDGE
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeErrors.OVERRIDING_GENERATED_BLOCKING_BRIDGE
import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeErrors.PLUGIN_IS_NOT_ENABLED
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers

object BlockingBridgeErrorsRendering : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("SerializationPlugin").apply {
        put(
            PLUGIN_IS_NOT_ENABLED,
            "JvmBlockingBridge compiler plugin is not applied to the module, so this annotation would not be processed. " +
                    "Make sure that you've setup your buildscript correctly and re-import project."
        )

        put(
            OVERRIDING_GENERATED_BLOCKING_BRIDGE,
            "Overriding generated JvmBlockingBridge: ''{0}''",
            Renderers.STRING
        )

        put(
            INAPPLICABLE_JVM_BLOCKING_BRIDGE,
            "@JvmBlockingBridge is inapplicable on this declaration"
        )
    }

    override fun getMap() = MAP
}
