package me.him188.kotlin.jvm.blocking.bridge.ide

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

internal object Icons {
    val BridgedSuspendCall: Icon = IconLoader.getIcon("/icons/bridgedSuspendCall.svg", Icons::class.java)
    val BridgedSuspendCallDark: Icon = IconLoader.getIcon("/icons/bridgedSuspendCall_dark.svg", Icons::class.java)
}