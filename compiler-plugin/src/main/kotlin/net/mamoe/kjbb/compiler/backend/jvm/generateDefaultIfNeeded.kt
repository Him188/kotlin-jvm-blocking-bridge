package net.mamoe.kjbb.compiler.backend.jvm

import org.jetbrains.kotlin.codegen.DefaultParameterValueLoader
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtNamedFunction


/*

    void generateDefaultIfNeeded(
            @NotNull MethodContext owner,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull OwnerKind kind,
            @NotNull DefaultParameterValueLoader loadStrategy,
            @Nullable KtNamedFunction function
    )
 */
private val GENERATE_DEFAULT_IF_NEEDED =
    FunctionCodegen::class.java.getDeclaredMethod(
        "generateDefaultIfNeeded",
        MethodContext::class.java,
        FunctionDescriptor::class.java,
        OwnerKind::class.java,
        DefaultParameterValueLoader::class.java,
        KtNamedFunction::class.java
    ).apply {
        kotlin.runCatching { isAccessible = true }
    }

internal fun FunctionCodegen.generateDefaultIfNeeded1(
    owner: MethodContext,
    functionDescriptor: FunctionDescriptor,
    kind: OwnerKind,
    loadStrategy: DefaultParameterValueLoader,
    function: KtNamedFunction?,
) {
    GENERATE_DEFAULT_IF_NEEDED(this, owner, functionDescriptor, kind, loadStrategy, function)
}