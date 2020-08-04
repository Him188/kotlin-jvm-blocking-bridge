package net.mamoe.kjbb.ide

import com.google.auto.service.AutoService
import net.mamoe.kjbb.compiler.backend.jvm.GeneratedBlockingBridgeStubForResolution
import net.mamoe.kjbb.compiler.backend.jvm.canGenerateJvmBlockingBridge
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

@AutoService(SyntheticResolveExtension::class)
class JvmBlockingBridgeResolveExtension : SyntheticResolveExtension {
    override fun getPossibleSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name>? {
        return super.getSyntheticNestedClassNames(thisDescriptor)
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        return
        if (name.isSpecial) return

        if (thisDescriptor.source !is KotlinSourceElement) {
            return
        }

        for (originFunction in result.toList()) {
            CompilerContextIntelliJ.run {
                if (originFunction.isGeneratedStubForJavaResolving()) {
                    result.remove(originFunction)
                }
            }

            if (originFunction.canGenerateJvmBlockingBridge()) {
                result.remove(originFunction)
                result.add(
                    JvmBlockingBridgeResolver.generateSyntheticMethods(
                        thisDescriptor,
                        name,
                        originFunction,
                        isSuspend = false,
                        addStubFlag = true
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
        originFunction: FunctionDescriptor,
        isSuspend: Boolean,
        addStubFlag: Boolean
        //  jvmNameClassDescriptor: ClassDescriptor
    ): SimpleFunctionDescriptor {

        return SimpleFunctionDescriptorImpl.create(
            thisDescriptor,
            originFunction.annotations,
            Name.identifier("${name.identifier}"),
            CallableMemberDescriptor.Kind.SYNTHESIZED, //
            originFunction.source
        ).apply {
            this.isSuspend = isSuspend
            val functionDescriptor = this
            initialize(
                originFunction.extensionReceiverParameter?.copy(functionDescriptor),
                thisDescriptor.thisAsReceiverParameter,
                originFunction.typeParameters,
                originFunction.valueParameters.map { it.copy(functionDescriptor, it.name, it.index) },
                originFunction.returnType,
                originFunction.modality,
                originFunction.visibility,
                if (!addStubFlag) mapOf() else mapOf<CallableDescriptor.UserDataKey<*>, Boolean>(
                    GeneratedBlockingBridgeStubForResolution to true
                )
            )
        }/*
        .newCopyBuilder().apply {
            setHiddenToOvercomeSignatureClash()
            setSignatureChange()
            setAdditionalAnnotations(
                Annotations.create(
                    listOf(
                        AnnotationDescriptorImpl(
                            thisDescriptor.module.resolveTopLevelClass(
                                JVM_NAME_FQ_NAME,
                                NoLookupLocation.WHEN_RESOLVE_DECLARATION
                            )!!.defaultType,
                            mapOf(Name.identifier("name") to StringValue(originFunction.jvmNameOrName.identifier)),
                            originFunction.source
                        )
                    )
                )
            )
            setHiddenForResolutionEverywhereBesideSupercalls()
        }.build()!!
        */
    }
}