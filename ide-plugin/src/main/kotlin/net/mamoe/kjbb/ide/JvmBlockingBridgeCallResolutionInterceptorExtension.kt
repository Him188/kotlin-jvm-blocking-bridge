@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package net.mamoe.kjbb.ide

import com.google.auto.service.AutoService
import net.mamoe.kjbb.compiler.backend.jvm.isGeneratedBlockingBridgeStub
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.extensions.internal.CallResolutionInterceptorExtension
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CandidateResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference
import org.jetbrains.kotlin.resolve.calls.tower.PSICallResolver
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo

/**
 * Hide
 */
@AutoService(CallResolutionInterceptorExtension::class)
@OptIn(InternalNonStableExtensionPoints::class)
class JvmBlockingBridgeCallResolutionInterceptorExtension : CallResolutionInterceptorExtension {
    @Suppress("OverridingDeprecatedMember")
    override fun interceptCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: CallResolver?,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> {
        return candidates
    }

    override fun interceptCandidates(
        candidates: Collection<NewResolutionOldInference.MyCandidate>,
        context: BasicCallResolutionContext,
        candidateResolver: CandidateResolver,
        callResolver: CallResolver,
        name: Name,
        kind: NewResolutionOldInference.ResolutionKind,
        tracing: TracingStrategy
    ): Collection<NewResolutionOldInference.MyCandidate> {
        return candidates
    }

    override fun interceptFunctionCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: CallResolver,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> {
        if (candidates.isEmpty()) return candidates

        return candidates.toMutableList().apply {
            removeAll {
                CompilerContextIntelliJ.run {
                    it.isGeneratedStubForJavaResolving()
                }
            }
        }

        return candidates.toMutableList().apply {
            removeIf { it.isGeneratedBlockingBridgeStub() }
        }
    }

    override fun interceptFunctionCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: PSICallResolver,
        name: Name,
        location: LookupLocation,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<FunctionDescriptor> = InterceptFunctionCandidatesExtensions(
        candidates, scopeTower, resolutionContext, resolutionScope,
        callResolver, name, location, dispatchReceiver, extensionReceiver
    ).run {
        if (candidates.isEmpty()) return candidates

        return candidates.toMutableList().apply {
            removeAll {
                CompilerContextIntelliJ.run {
                    it.isGeneratedStubForJavaResolving()
                }
            }
        }
    }

    override fun interceptVariableCandidates(
        candidates: Collection<VariableDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: CallResolver,
        name: Name,
        location: LookupLocation
    ): Collection<VariableDescriptor> {
        return candidates
    }

    override fun interceptVariableCandidates(
        candidates: Collection<VariableDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: PSICallResolver,
        name: Name,
        location: LookupLocation,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<VariableDescriptor> {
        return candidates
    }
}

class InterceptFunctionCandidatesExtensions(
    private val candidates: Collection<FunctionDescriptor>,
    private val scopeTower: ImplicitScopeTower,
    private val resolutionContext: BasicCallResolutionContext,
    private val resolutionScope: ResolutionScope,
    private val callResolver: PSICallResolver,
    private val name: Name,
    private val location: LookupLocation,
    private val dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    private val extensionReceiver: ReceiverValueWithSmartCastInfo?
) {

    fun findClass(name: Name, location: LookupLocation = this.location) =
        findClassifier(name, location) as? ClassDescriptor

    fun findClassifier(name: Name, location: LookupLocation = this.location): ClassifierDescriptor? =
        resolutionScope.getContributedClassifierIncludeDeprecated(name, location)?.descriptor
}
