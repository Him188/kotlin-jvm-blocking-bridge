package net.mamoe.kjbb.compiler.extensions

import com.google.auto.service.AutoService
import net.mamoe.kjbb.compiler.backend.jvm.canGenerateJvmBlockingBridge
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

/**
 * Generate synthetic
 */
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
        if (name.isSpecial) return

        if (thisDescriptor.source !is KotlinSourceElement) {
            return
        }

        for (originFunction in result.toList()) {
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

val FunctionDescriptor.jvmName: String?
    get() = annotations.findAnnotation(JVM_NAME_FQ_NAME)
        ?.argumentValue("name")
        ?.value as String?

val FunctionDescriptor.jvmNameOrName: Name
    get() = jvmName?.let { Name.identifier(it) } ?: name

private val JVM_NAME_FQ_NAME = FqName(JvmName::class.qualifiedName!!)

/**
 * For ignoring
 */
object GeneratedBlockingBridgeStubForResolution : CallableDescriptor.UserDataKey<Boolean>

fun FunctionDescriptor.isGeneratedBlockingBridgeStub(): Boolean =
    this.getUserData(GeneratedBlockingBridgeStubForResolution) == true