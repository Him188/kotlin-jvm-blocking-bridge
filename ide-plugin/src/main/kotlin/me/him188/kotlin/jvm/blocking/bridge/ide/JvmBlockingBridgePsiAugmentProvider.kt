package me.him188.kotlin.jvm.blocking.bridge.ide

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.*
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.ir.RuntimeIntrinsics
import me.him188.kotlin.jvm.blocking.bridge.compiler.backend.jvm.HasJvmBlockingBridgeAnnotation
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

/**
 * Allows inserting elements into a PsiElement
 */
class JvmBlockingBridgePsiAugmentProvider : PsiAugmentProvider() {
    override fun <Psi : PsiElement?> getAugments(
        element: PsiElement,
        type: Class<Psi>,
        nameHint: String?
    ): MutableList<Psi> {
        if (element !is KtUltraLightClass && element !is KtUltraLightClassForFacade) return mutableListOf()
        if (type != PsiMethod::class.java) {
            return mutableListOf()
        }

        element as PsiExtensibleClass // inference mistake

        val ret =
            CachedValuesManager.getCachedValue(
                element,
                JvmBlockingBridgeCachedValueProvider(element) { element.generateAugmentElements(element.ownMethods) }
            ).orEmpty().toMutableList()
        @Suppress("UNCHECKED_CAST")
        return ret as MutableList<Psi>
    }

    // Note: keep this for compatibility with old IDEs. The new overload comes with IDEA 222
    @Deprecated("Deprecated in Java", ReplaceWith("getAugments(element, type, null)"))
    override fun <Psi : PsiElement?> getAugments(element: PsiElement, type: Class<Psi>): MutableList<Psi> {
        return getAugments(element, type, null)
    }

    private class JvmBlockingBridgeCachedValueProvider(
        private val element: PsiElement,
        private val psiAugmentGenerator: () -> List<PsiElement>,
    ) : CachedValueProvider<List<PsiElement>> {
        companion object {
            internal val guard = RecursionManager.createGuard<PsiElement>("kjbb.augment")
        }

        override fun compute(): CachedValueProvider.Result<List<PsiElement>>? {
            return guard.doPreventingRecursion(element, true) {
                CachedValueProvider.Result.create(psiAugmentGenerator(), element)
            }
        }
    }

}

internal fun PsiElement.generateAugmentElements(ownMethods: List<PsiMethod>): List<PsiElement> {
    if (!this.isBridgeCompilerEnabled) {
        return emptyList()
    }

    /*
    val originalElement = this.originalElement
    if (originalElement is KtUltraLightClass) {
        val companions = originalElement.kotlinOrigin.companionObjects
        result.addAll(companions.flatMap { companion ->
            companion.declarations
                .asSequence()
                .map { it.getRepresentativeLightMethod() }
                .filterIsInstance<KtLightMethod>()
                .filter { it.isJvmStaticInCompanion() }
                .flatMap { it.generateLightMethod(it.containingClass, true).asSequence() }
                .toList()
        })
        result.clear()
    }*/

    return ownMethods.asSequence()
        .filterIsInstance<KtLightMethod>()
        .filter { it.canHaveBridgeFunctions(this.isIr).generate }
        .flatMap { it.generateLightMethod(it.containingClass).asSequence() }
        .toList()
}

internal fun KtAnnotated.findAnnotation(fqName: FqName): KtAnnotationEntry? {
    val analyze = this.analyze()
    for (entry in annotationEntries) {
        if (entry == null) continue
        val annotation = analyze.get(BindingContext.ANNOTATION, entry) ?: continue
        if (annotation.fqName == fqName) return entry
    }
    return null
}

internal val KtLightMethod.isTopLevel get() = this.kotlinOrigin?.containingClassOrObject == null

internal fun PsiMethod.canHaveBridgeFunctions(isIr: Boolean): HasJvmBlockingBridgeAnnotation {
    if (this is BlockingBridgeStubMethod) return HasJvmBlockingBridgeAnnotation.NONE
    if (this !is KtLightMethod) return HasJvmBlockingBridgeAnnotation.NONE
    if (!isSuspend()) return HasJvmBlockingBridgeAnnotation.NONE
    if (!Name.isValidIdentifier(this.name)) return HasJvmBlockingBridgeAnnotation.NONE

    if (this.hasAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName.asString())) return HasJvmBlockingBridgeAnnotation.FROM_FUNCTION

    // no @JvmBlockingBridge on function, check if it has on class or file.

    val descriptor = this.kotlinOrigin?.descriptor as? SimpleFunctionDescriptor
    if (descriptor != null) {
        if (!descriptor.effectiveVisibility(checkPublishedApi = true).publicApi) {
            return HasJvmBlockingBridgeAnnotation.NONE
        }
        if (descriptor.containingDeclaration !is ClassDescriptor && !isIr) {
            return HasJvmBlockingBridgeAnnotation.NONE
        }
    }

    if (this.isTopLevel) {
        if (!isIr) return HasJvmBlockingBridgeAnnotation.NONE
    } else {
        if (containingClass.hasAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName.asString()))
            return HasJvmBlockingBridgeAnnotation.FROM_CONTAINING_DECLARATION
    }

    if (bridgeConfiguration.enableForModule) return HasJvmBlockingBridgeAnnotation.ENABLE_FOR_MODULE

    if (containingKtFile?.findAnnotation(RuntimeIntrinsics.JvmBlockingBridgeFqName) != null) return HasJvmBlockingBridgeAnnotation.FROM_CONTAINING_DECLARATION

    val fromSuper = findOverrides()?.map { it.canHaveBridgeFunctions(isIr) }?.firstOrNull { it.generate }
    if (fromSuper?.generate == true) return fromSuper

    return HasJvmBlockingBridgeAnnotation.NONE
}

/**
 * @return `null` if top-level method
 */
internal fun PsiMethod.findOverrides(): Sequence<PsiMethod>? {
    return containingClass?.superClasses
        ?.flatMap { it.methods.asSequence() }
        ?.filter {
            it.hasSameSignatureWith(this)
        }
}

internal fun PsiMethod.hasSameSignatureWith(another: PsiMethod): Boolean {
    return this.hierarchicalMethodSignature == another.hierarchicalMethodSignature
}

internal val PsiClass.superClasses: Sequence<PsiClass> get() = this.superTypes.asSequence().mapNotNull { it.resolve() }

internal fun PsiMethod.isSuspend(): Boolean =
    this.modifierList.text.contains("suspend")

internal fun PsiMethod.isJvmStatic(): Boolean = hasAnnotation(JvmStatic::class.qualifiedName!!)

internal fun KtLightMethod.generateLightMethod(
    containingClass: KtLightClass,
): List<PsiMethod> {
    ProgressManager.checkCanceled()
    val originMethod = this

    fun generateImpl(
        parameters: Map<KtParameter, PsiParameter>,
        originalElement: KtLightMethod,
        kotlinOriginKind: JvmDeclarationOriginKind,
    ): BlockingBridgeStubMethod {
        val kotlinOrigin = originalElement.kotlinOrigin?.let { kotlinOrigin ->
            LightMemberOriginForDeclaration(kotlinOrigin, kotlinOriginKind, parameters.keys.toList())
        }

        return BlockingBridgeStubMethod(kotlinOrigin, containingClass, originMethod, parameters)
    }

    val overloads = (originMethod.kotlinOrigin as? KtNamedFunction)?.valueParameters // last is Continuation
        ?.jvmOverloads(originMethod.parameterList) ?: return emptyList()

    if (overloads.isEmpty()) return emptyList()

    val baseMethodParameters = overloads.first()

    val baseMethod =
        generateImpl(baseMethodParameters, originMethod, JvmDeclarationOriginKind.OTHER)

    return overloads.map {
        if (it === baseMethodParameters) {
            baseMethod
        } else {
            generateImpl(it, baseMethod, JvmDeclarationOriginKind.JVM_OVERLOADS)
        }
    }
}

private fun List<KtParameter>.jvmOverloads(parameterList: PsiParameterList): List<Map<KtParameter, PsiParameter>>? {
    fun findPsiParameter(name: String?): PsiParameter? {
        return parameterList.parameters.find { it.name == name }
    }

    fun takeNDefaults(n: Int): List<KtParameter> {
        val list = mutableListOf<KtParameter>()
        for (ktParameter in this) {
            if (ktParameter.hasDefaultValue()) {
                if (list.size < n) {
                    list.add(ktParameter)
                }
            } else {
                list.add(ktParameter)
            }
        }
        return list
    }

    val defaultValueCount = this.count { it.hasDefaultValue() }
    if (defaultValueCount == 0) return listOf(this.associateWith { findPsiParameter(it.name) ?: return null })

    val result = mutableListOf<Map<KtParameter, PsiParameter>>()
    for (count in defaultValueCount downTo 0) {
        result += takeNDefaults(count).associateWith { findPsiParameter(it.name) ?: return null }
    }
    return result
}

internal class BlockingBridgeStubMethod(
    lightMemberOrigin: LightMemberOriginForDeclaration?,
    containingClass: KtLightClass,
    private val originalSuspendFunction: KtLightMethod,
    private val parameters: Map<KtParameter, PsiParameter>,
) : KtLightMethodImpl(lightMemberOrigin, containingClass) {

    override fun buildParametersForList(): List<PsiParameter> {
        return parameters.values.toList()
//        return originalSuspendFunction.parameterList.parameters.toList().dropLast(1) // Drop `$completion`
    }

    override fun buildTypeParameterList(): PsiTypeParameterList? = originalSuspendFunction.typeParameterList

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Deprecated in Java")
    override fun findDeepestSuperMethod(): PsiMethod? = PsiSuperMethodImplUtil.findDeepestSuperMethod(this)

    override fun findDeepestSuperMethods(): Array<out PsiMethod> = PsiSuperMethodImplUtil.findDeepestSuperMethods(this)

    override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean): List<MethodSignatureBackedByPsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess)

    override fun findSuperMethods(): Array<out PsiMethod> = PsiSuperMethodImplUtil.findSuperMethods(this)
    override fun findSuperMethods(parentClass: PsiClass?): Array<out PsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethods(this, parentClass)

    override fun findSuperMethods(checkAccess: Boolean): Array<out PsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess)

    override fun getDefaultValue(): PsiAnnotationMemberValue? = null
    override fun getDocComment(): PsiDocComment? = originalSuspendFunction.docComment
    override fun getHierarchicalMethodSignature(): HierarchicalMethodSignature =
        PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this)

    private val _modifierList by lazy {
        LightModifierList(manager, language).apply {
            if (originalSuspendFunction.isJvmStatic()) {
                addModifier(PsiModifier.STATIC)
            }

            VISIBILITIES_MODIFIERS
                .filter { originalSuspendFunction.hasModifierProperty(it) }
                .forEach { addModifier(it) }

            addModifier(
                if (containingClass.isInterface) {
                    PsiModifier.OPEN
                } else when (containingClass.modality) {
                    Modality.OPEN, Modality.ABSTRACT, Modality.SEALED -> PsiModifier.OPEN
                    else -> PsiModifier.FINAL
                }
            )
        }
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun getName(): String = originalSuspendFunction.name


    // must compute immediately, otherwise there will be StackOverflowError. I don't know why.
    private val _returnType = computeReturnType()

    private fun computeReturnType(): PsiType? {
        val continuationParamType = originalSuspendFunction.hierarchicalMethodSignature.parameterTypes.lastOrNull()
            ?: return null

        val psiClassReferenceType = continuationParamType as? PsiClassReferenceType ?: return null

        return when (val type = psiClassReferenceType.parameters.getOrNull(0) ?: return null) {
            is PsiWildcardType -> { // ? super String
                (type.bound ?: type).coerceUnitToVoid()
            }

            else -> {
                type.coerceUnitToVoid()
            }
        }
    }

    override fun getReturnType(): PsiType? = _returnType

    override fun getReturnTypeElement(): PsiTypeElement? = originalSuspendFunction.returnTypeElement

    override fun getSignature(substitutor: PsiSubstitutor): MethodSignature {
        return MethodSignatureBackedByPsiMethod.create(this, substitutor)
    }

    override fun getThrowsList(): PsiReferenceList = originalSuspendFunction.throwsList
    override fun hasModifierProperty(name: String): Boolean = originalSuspendFunction.hasModifierProperty(name)
    override fun isConstructor(): Boolean = originalSuspendFunction.isConstructor
    override fun isDeprecated(): Boolean = originalSuspendFunction.isDeprecated
    override fun isVarArgs(): Boolean = originalSuspendFunction.isVarArgs
}

private fun PsiType.coerceUnitToVoid(): PsiType {
    return if (this.canonicalText == "kotlin.Unit") PsiType.VOID else this
}

internal val VISIBILITIES_MODIFIERS = arrayOf(
    PsiModifier.PUBLIC,
    PsiModifier.PACKAGE_LOCAL,
    PsiModifier.PRIVATE,
    PsiModifier.PROTECTED,
)

internal val PsiModifierListOwner.modality: Modality
    get() {
        if (this is PsiMember && this.containingClass?.isInterface == true) {
            return Modality.OPEN //
        }

        val modifierList = this.modifierList?.text

        return when {
            modifierList == null -> return Modality.FINAL
            modifierList.contains("open") -> Modality.OPEN
            modifierList.contains("final") -> Modality.FINAL
            modifierList.contains("abstract") -> Modality.ABSTRACT
            modifierList.contains("sealed") -> Modality.ABSTRACT
            else -> Modality.FINAL
        }
    }
