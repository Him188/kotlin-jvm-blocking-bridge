package me.him188.kotlin.jvm.blocking.bridge.compiler.extensions

import com.google.auto.service.AutoService
import me.him188.kotlin.jvm.blocking.bridge.compiler.JvmBlockingBridgeCompilerConfigurationKeys.ENABLE_FOR_MODULE
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
class BridgeCommandLineProcessor : CommandLineProcessor {
    companion object {
        const val COMPILER_PLUGIN_ID: String = "kotlin-jvm-blocking-bridge"

        val OPTION_ENABLE_FOR_MODULE: CliOption = CliOption(
            ENABLE_FOR_MODULE.name,
            "<true|false>",
            "Generate blocking bridges for all effectively public suspend functions in the module where possible."
        )
    }

    override val pluginId: String = COMPILER_PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf(OPTION_ENABLE_FOR_MODULE)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            OPTION_ENABLE_FOR_MODULE -> configuration.put(ENABLE_FOR_MODULE, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }
}