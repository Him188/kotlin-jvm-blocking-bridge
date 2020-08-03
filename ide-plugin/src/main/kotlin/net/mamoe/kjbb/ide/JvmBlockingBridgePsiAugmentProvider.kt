package net.mamoe.kjbb.ide

import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import net.mamoe.kjbb.JvmBlockingBridge
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import kotlin.contracts.contract

/**
 * Allows inserting elements into a PsiElement
 */
class JvmBlockingBridgePsiAugmentProvider : PsiAugmentProvider() {
    @Suppress("UNCHECKED_CAST")
    override fun <Psi : PsiElement?> getAugments(element: PsiElement, type: Class<Psi>): MutableList<Psi> {

        if (element !is KtUltraLightClass) return mutableListOf()
        if (type != PsiMethod::class.java) {
            return mutableListOf()
        }

        val ret =
            CachedValuesManager.getCachedValue(
                element,
                JvmBlockingBridgeCachedValueProvider(element, element::generateAugmentElements)
            ).toMutableList()
        return ret as MutableList<Psi>
    }

    private class JvmBlockingBridgeCachedValueProvider(
        private val element: PsiElement,
        private val psiAugmentGenerator: () -> List<PsiElement>
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
        .filter(PsiMethod::canHaveBlockingBridge)
        .filterIsInstance<KtLightMethod>()
        .map(KtLightMethod::generateLightMethod)
        .toList()
}

internal fun PsiMethod.canHaveBlockingBridge(): Boolean {
    contract {
        returns(true) implies (this@canHaveBlockingBridge is KtLightMethod)
    }
    return this is KtLightMethod && isSuspend() && this.hasAnnotation(JvmBlockingBridge::class.qualifiedName!!)
}

internal fun KtLightMethod.isSuspend(): Boolean =
    this.modifierList.text.contains("suspend")

internal fun KtLightMethod.isJvmStatic(): Boolean = hasAnnotation(JvmStatic::class.qualifiedName!!)

internal fun KtLightMethod.generateLightMethod(): PsiMethod {
    val originMethod = this

    return LightMethodBuilder(
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

        JvmModifier.values()
            .filter { originMethod.hasModifierProperty(it.name) }
            .forEach { addModifier(it.name.toLowerCase()) }

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
    }
}

private enum class JvmModifier {
    PUBLIC, PROTECTED, PRIVATE, PACKAGE_LOCAL, STATIC, ABSTRACT, FINAL, NATIVE, SYNCHRONIZED, STRICTFP, TRANSIENT, VOLATILE, TRANSITIVE
}