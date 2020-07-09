package net.mamoe.kjbb.jvm

import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.SpecialNames

class BridgeCodegen(
    private val codegen: ImplementationBodyCodegen
) {
    private val clazz = codegen.descriptor


    fun generate() {
        val members = clazz.unsubstitutedMemberScope
        val names = members.getFunctionNames()
        val functions =
            names.flatMap { members.getContributedFunctions(it, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS) }.toSet()

        for (function in functions) {
            function.generateBridge()
        }
    }

    fun SimpleFunctionDescriptor.generateBridge() {
        println("generating bridge: ${this.valueParameters}, ${this.isSuspend}")
        val desc = AnonymousFunctionDescriptor.create(
            clazz,
            Annotations.EMPTY,
            SpecialNames.NO_NAME_PROVIDED,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            source
        ).apply {
            initialize(
                null,
                ReceiverParameterDescriptorImpl(clazz, clazz.thisAsReceiverParameter.value, Annotations.EMPTY),
                typeParameters,
                valueParameters.map { it.copy(this, it.name, it.index) },
                returnType,
                null,
                visibility
            )

            isSuspend = true
        }

        //CoroutineCodegenForNamedFunction()

        codegen.generateMethod(
            desc
        ) { jvmMethodSignature, expressionCodegen ->

        }
    }
}