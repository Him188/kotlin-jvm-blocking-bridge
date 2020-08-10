package net.mamoe.kjbb.ide

import com.intellij.lang.Language
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.*
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import net.mamoe.kjbb.JvmBlockingBridge
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingMethod
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

/**
 * Allows inserting elements into a PsiElement
 */
class JvmBlockingBridgePsiAugmentProvider : PsiAugmentProvider() {
    @Suppress("UNCHECKED_CAST")
    override fun <Psi : PsiElement?> getAugments(element: PsiElement, type: Class<Psi>): MutableList<Psi> {

        if (element !is KtUltraLightClass && element !is KtUltraLightClassForFacade) return mutableListOf()
        if (type != PsiMethod::class.java) {
            return mutableListOf()
        }

        element as PsiExtensibleClass // inference mistake

        val ret =
            CachedValuesManager.getCachedValue(
                element,
                JvmBlockingBridgeCachedValueProvider(element) { element.generateAugmentElements() }
            ).orEmpty().toMutableList()
        return ret as MutableList<Psi>
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

internal fun PsiExtensibleClass.generateAugmentElements(): List<PsiElement> {
    return this.ownMethods.asSequence()
        .filter { it.isCompanionedWithBlockingBrideInThisOrSuper() }
        .filterIsInstance<KtLightMethod>()
        .map { it.generateLightMethod() }
        .toList()
}

internal fun PsiMethod.isCompanionedWithBlockingBrideInThisOrSuper(): Boolean {
    return if (isSuspend()
        && Name.isValidIdentifier(this.name)
        && this.hasAnnotation(JvmBlockingBridge::class.qualifiedName!!)
    ) {
        true
    } else {
        return findOverrides()?.any { it.isCompanionedWithBlockingBrideInThisOrSuper() } == true
    }
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

internal fun PsiMethod.hasOverridingMethod(): Boolean {
    var any = false
    forEachOverridingMethod {
        any = true
        false
    }
    return any
}

internal fun PsiMethod.firstOverridingMethodOrNull(): PsiMethod? {
    var any: PsiMethod? = null
    forEachOverridingMethod {
        any = it
        false
    }
    return any
}

internal val PsiMethod.overridingMethods: List<PsiMethod>
    get() {
        val mutableList = mutableListOf<PsiMethod>()
        forEachOverridingMethod {
            mutableList.add(it)
            true
        }
        return mutableList
    }

internal fun PsiMethod.isSuspend(): Boolean =
    this.modifierList.text.contains("suspend")

internal fun PsiMethod.isJvmStatic(): Boolean = hasAnnotation(JvmStatic::class.qualifiedName!!)

internal fun KtLightMethod.generateLightMethod(): PsiMethod {
    val originMethod = this

    return BlockingBridgeStubMethod(
        originMethod.manager,
        originMethod.language,
        originMethod.name
    ).apply {
        for (it in originMethod.parameterList.parameters.dropLast(1)) {
            addParameter(it)
        }
        if (isJvmStatic()) {
            addModifier(PsiModifier.STATIC)
        }

        PsiModifier.MODIFIERS
            .filter { originMethod.hasModifierProperty(it) }
            .forEach { addModifier(it) }

        for (typeParameter in originMethod.typeParameters) {
            addTypeParameter(typeParameter)
        }

        for (referenceElement in originMethod.throwsList.referenceElements) {
            addException(referenceElement.qualifiedName)
        }

        originMethod.hierarchicalMethodSignature.parameterTypes.last().let { continuationParamType ->
            val psiClassReferenceType = continuationParamType as PsiClassReferenceType

            when (val type = psiClassReferenceType.parameters[0]) {
                is PsiWildcardType -> {
                    setMethodReturnType(type.bound)
                }
                else -> {
                    setMethodReturnType(type.canonicalText)
                }
            }
        }

        this.containingClass = originMethod.containingClass

        navigationElement = originMethod

        setBody(JavaPsiFacade.getElementFactory(project).createCodeBlock())
    }.let {
        val kotlinOrigin = originMethod.kotlinOrigin?.let { kotlinOrigin ->
            LightMemberOriginForDeclaration(kotlinOrigin, JvmDeclarationOriginKind.BRIDGE) // // TODO: 2020/8/4
        }
        KtLightMethodImpl.create(it, kotlinOrigin, originMethod.containingClass).apply {

        }
    }
}


class BlockingBridgeStubMethod(manager: PsiManager, language: Language, name: String) :
    LightMethodBuilder(manager, language, name) {

    private var _body: PsiCodeBlock? = null

    fun setBody(body: PsiCodeBlock) {
        _body = body
    }

    override fun getBody(): PsiCodeBlock? {
        return _body ?: super.getBody()
    }

}