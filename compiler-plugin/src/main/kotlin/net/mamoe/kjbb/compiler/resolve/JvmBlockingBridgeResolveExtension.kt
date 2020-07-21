package net.mamoe.kjbb.compiler.resolve

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

/**
 * Generate synthetic
 */
class JvmBlockingBridgeResolveExtension : SyntheticResolveExtension {

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        result.add(JvmBlockingBridgeResolver.generateSyntheticMethods(thisDescriptor, name))
    }
}

object JvmBlockingBridgeResolver {
    fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name
    ): SimpleFunctionDescriptor {
        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            thisDescriptor, Annotations.EMPTY, name, CallableMemberDescriptor.Kind.SYNTHESIZED, thisDescriptor.source
        )

        val originFunction = thisDescriptor.unsubstitutedMemberScope
            .getContributedFunctions(name, NoLookupLocation.FROM_BUILTINS).single()

        functionDescriptor.initialize(
            originFunction.extensionReceiverParameter?.copy(functionDescriptor),
            thisDescriptor.thisAsReceiverParameter,
            originFunction.typeParameters,
            originFunction.valueParameters.map { it.copy(functionDescriptor, it.name, it.index) },
            originFunction.returnType,
            originFunction.modality,
            originFunction.visibility
        )

        return functionDescriptor
    }
}