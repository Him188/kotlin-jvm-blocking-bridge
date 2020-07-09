@file:Suppress(
    "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "EXPERIMENTAL_API_USAGE",
    "EXPERIMENTAL_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_USE_EXPERIMENTAL"
)

package net.mamoe.kjbb.jvm

import net.mamoe.kjbb.GeneratedBlockingBridge
import net.mamoe.kjbb.JvmBlockingBridge
import org.jetbrains.org.objectweb.asm.Type

internal val jvmBlockingBridgeType = Type.getObjectType(JvmBlockingBridge::class.qualifiedName!!.replace('.', '/'))
internal val generatedBlockingBridgeType =
    Type.getObjectType(GeneratedBlockingBridge::class.qualifiedName!!.replace('.', '/'))

