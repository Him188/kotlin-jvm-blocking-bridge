package net.mamoe.kjbb.compiler.diagnostic

import net.mamoe.kjbb.compiler.diagnostic.BlockingBridgeErrors.*
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers

object BlockingBridgeErrorsRendering : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("SerializationPlugin").apply {
        /*put(
            PLUGIN_IS_NOT_ENABLED,
            "JvmBlockingBridge compiler plugin is not applied to the module, so this annotation would not be processed. " +
                    "Make sure that you've setup your buildscript correctly and re-import project."
        )*/

        put(
            OVERRIDING_GENERATED_BLOCKING_BRIDGE,
            "Overriding generated JvmBlockingBridge: ''{0}''.",
            Renderers.STRING
        )

        put(
            REDUNDANT_JVM_BLOCKING_BRIDGE_ON_PRIVATE_DECLARATIONS,
            "@JvmBlockingBridge is redundant on private declarations, as generated bridges are also private and can't be resolved from Java."
        )

        put(
            INAPPLICABLE_JVM_BLOCKING_BRIDGE,
            "@JvmBlockingBridge is not applicable on this declaration."
        )

        put(
            INLINE_CLASSES_NOT_SUPPORTED,
            "Inline class is not supported for ''{0}''.",
            Renderers.DECLARATION_NAME_WITH_KIND
        )

        put(
            INTERFACE_NOT_SUPPORTED,
            "Interface is not supported for jvm target lower than 8."
        )

        put(
            TOP_LEVEL_FUNCTIONS_NOT_SUPPORTED,
            "Top-level functions are not yet supported with the legacy JVM compiler backend."
        )
    }

    override fun getMap() = MAP
}
