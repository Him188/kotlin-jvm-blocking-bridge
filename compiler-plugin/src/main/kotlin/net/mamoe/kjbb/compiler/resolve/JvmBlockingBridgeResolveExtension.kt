package net.mamoe.kjbb.compiler.resolve

import net.mamoe.kjbb.compiler.backend.jvm.canGenerateJvmBlockingBridge
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.types.asSimpleType

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
        if (name.isSpecial) return

        for (originFunction in result.toList()) {
            if (originFunction.canGenerateJvmBlockingBridge()) {
                result.add(
                    JvmBlockingBridgeResolver.generateSyntheticMethods(
                        thisDescriptor,
                        name,
                        originFunction,
                        thisDescriptor.builtIns.getBuiltInClassByFqName(JVM_NAME_FQ_NAME)
                    )
                )
            }
        }
    }
}

object JvmBlockingBridgeResolver {
    fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        originFunction: SimpleFunctionDescriptor,
        jvmNameClassDescriptor: ClassDescriptor
    ): SimpleFunctionDescriptor {

        fun Annotations.optimizeForGeneratedStub(): Annotations {
            val all = this.toMutableList()

            val jvmName =
                all.firstOrNull { it.fqName == JVM_NAME_FQ_NAME }?.allValueArguments?.values?.firstOrNull()?.value as? String
                    ?: originFunction.name.identifier

            all.removeIf { it.fqName == JVM_NAME_FQ_NAME }

            all.add(
                AnnotationDescriptorImpl(
                    jvmNameClassDescriptor.defaultType.asSimpleType(),
                    mapOf(Name.identifier("name") to StringValue(jvmName)), originFunction.source
                )
            )

            return Annotations.create(all)
        }

        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            thisDescriptor,
            originFunction.annotations.optimizeForGeneratedStub(),
            Name.identifier("${name.identifier}_"),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            originFunction.source
        )

        functionDescriptor.initialize(
            originFunction.extensionReceiverParameter?.copy(functionDescriptor),
            thisDescriptor.thisAsReceiverParameter,
            originFunction.typeParameters,
            originFunction.valueParameters.map { it.copy(functionDescriptor, it.name, it.index) },
            originFunction.returnType,
            originFunction.modality,
            originFunction.visibility,
            mapOf<CallableDescriptor.UserDataKey<*>, Boolean>(
                GeneratedBlockingBridgeStubForResolution to true
            )
        )

        return functionDescriptor
    }
}

private val JVM_NAME_FQ_NAME = FqName(JvmName::class.qualifiedName!!)

/**
 * For ignoring
 */
object GeneratedBlockingBridgeStubForResolution : CallableDescriptor.UserDataKey<Boolean>