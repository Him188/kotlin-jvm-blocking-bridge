package net.mamoe.kjbb.ide

import com.intellij.lang.Language
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.*
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import net.mamoe.kjbb.JvmBlockingBridge
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.caches.project.toDescriptor
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingMethod
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtObjectDeclaration
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
                JvmBlockingBridgeCachedValueProvider(element) { element.generateAugmentElements(element.ownMethods) }
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

internal fun PsiElement.generateAugmentElements(ownMethods: List<PsiMethod>): List<PsiElement> {
    val result = ArrayList<PsiElement>()

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

    return result + ownMethods.asSequence()
        .filter { it.canHaveBridgeFunctions() }
        .filterIsInstance<KtLightMethod>()
        .flatMap { it.generateLightMethod(it.containingClass).asSequence() }
        .toList()
}

internal fun PsiMethod.canHaveBridgeFunctions(): Boolean {
    if (this.module?.toDescriptor()?.isBlockingBridgePluginEnabled() == false) {
        return false
    }
    if (!isSuspend()) return false
    return if (Name.isValidIdentifier(this.name)
        && this.hasAnnotation(JvmBlockingBridge::class.qualifiedName!!)
    ) {
        true
    } else {
        return findOverrides()?.any { it.canHaveBridgeFunctions() } == true
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

internal fun KtLightMethod.isJvmStaticInNonCompanionObject(): Boolean {
    val containingKotlinOrigin = containingClass.kotlinOrigin

    return hasAnnotation(JvmStatic::class.qualifiedName!!)
            && containingKotlinOrigin is KtObjectDeclaration
            && !containingKotlinOrigin.isCompanion()
}

internal fun KtLightMethod.isJvmStaticInCompanion(): Boolean {
    val containingKotlinOrigin = containingClass.kotlinOrigin

    return hasAnnotation(JvmStatic::class.qualifiedName!!)
            && containingKotlinOrigin is KtObjectDeclaration
            && containingKotlinOrigin.isCompanion()
}

internal fun KtLightMethod.generateLightMethod(
    containingClass: KtLightClass,
    generateAsStatic: Boolean = this.isJvmStatic(),
): List<PsiMethod> {
    ProgressManager.checkCanceled()
    val originMethod = this

    fun generateImpl(): PsiMethod? {
        val javaMethod = BlockingBridgeStubMethodBuilder(
            originMethod.manager,
            originMethod.language,
            originMethod.name,
            originMethod
        ).apply {
            this.containingClass = containingClass
            docComment = originMethod.docComment
            navigationElement = originMethod


            for (it in originMethod.parameterList.parameters.dropLast(1)) {
                addParameter(it)
            }

            if (generateAsStatic) {
                addModifier(PsiModifier.STATIC)
            }

            originMethod.annotations.forEach { annotation ->
                if (annotation.hasQualifiedName("kotlin.Deprecated"))
                    deprecated = true

                addAnnotation(annotation)
            }

            VISIBILITIES_MODIFIERS
                .filter { originMethod.hasModifierProperty(it) }
                .forEach { addModifier(it) }

            addModifier(
                if (containingClass.isInterface) {
                    PsiModifier.OPEN
                } else when (containingClass.modality) {
                    Modality.OPEN, Modality.ABSTRACT, Modality.SEALED -> PsiModifier.OPEN
                    else -> PsiModifier.FINAL
                }
            )

            for (typeParameter in originMethod.typeParameters) {
                addTypeParameter(typeParameter)
            }

            for (referenceElement in originMethod.throwsList.referenceElements) {
                addException(referenceElement.qualifiedName)
            }

            ProgressManager.checkCanceled()
            originMethod.hierarchicalMethodSignature.parameterTypes.lastOrNull().let { it ?: return null }
                .let { continuationParamType ->
                    val psiClassReferenceType = continuationParamType as? PsiClassReferenceType ?: return null

                    when (val type = psiClassReferenceType.parameters.getOrNull(0) ?: return null) {
                        is PsiWildcardType -> { // ? super String
                            setMethodReturnType(type.bound?.canonicalText ?: type.canonicalText)
                        }
                        else -> {
                            setMethodReturnType(type.canonicalText)
                        }
                    }
                }

            setBody(JavaPsiFacade.getElementFactory(project).createCodeBlock())
        }
        val kotlinOrigin = originMethod.kotlinOrigin?.let { kotlinOrigin ->
            LightMemberOriginForDeclaration(kotlinOrigin, JvmDeclarationOriginKind.BRIDGE) // // TODO: 2020/8/4
        }

        return BlockingBridgeStubMethod({ javaMethod }, kotlinOrigin, containingClass).apply {
        }
//            KtLightMethodImpl.create(it, kotlinOrigin, containingClass).apply {
//                this.returnType
//            }
    }

    return listOfNotNull(generateImpl())
}


class BlockingBridgeStubMethod(
    computeRealDelegate: () -> PsiMethod, lightMemberOrigin: LightMemberOrigin?, containingClass: KtLightClass,
) : KtLightMethodImpl(computeRealDelegate, lightMemberOrigin, containingClass) {
    override fun getReturnType(): PsiType? {
        return clsDelegate.returnType
    }

    override fun getReturnTypeElement(): PsiTypeElement? {
        return clsDelegate.returnTypeElement
    }
}

private class BlockingBridgeStubMethodBuilder(
    manager: PsiManager, language: Language, name: String, private val originalElement: PsiElement,
) : LightMethodBuilder(manager, language, name) {

    private var _body: PsiCodeBlock? = null
    private var _annotations: Array<PsiAnnotation> = emptyArray()
    private var docComment: PsiDocComment? = null

    fun setBody(body: PsiCodeBlock) {
        _body = body
    }

    override fun getOriginalElement(): PsiElement {
        return this.originalElement
    }

    override fun getBody(): PsiCodeBlock? {
        return _body ?: super.getBody()
    }

    fun addAnnotation(annotation: PsiAnnotation) {
        _annotations += annotation
    }

    override fun getDocComment(): PsiDocComment? {
        return docComment
    }

    fun setDocComment(docComment: PsiDocComment?) {
        this.docComment = docComment
    }

    var deprecated = false

    override fun isDeprecated(): Boolean {
        return deprecated
    }

    override fun getAnnotations(): Array<PsiAnnotation> = _annotations
    override fun hasAnnotation(fqn: String): Boolean {
        return _annotations.any { it.hasQualifiedName(fqn) }
    }

    override fun getAnnotation(fqn: String): PsiAnnotation? {
        return _annotations.find { it.hasQualifiedName(fqn) }
    }
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
