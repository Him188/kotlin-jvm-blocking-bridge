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
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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

internal fun PsiMethod.isJvmOverloads(): Boolean = hasAnnotation(JvmOverloads::class.qualifiedName!!)

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

    fun generateImpl(
        parameters: List<PsiParameter>,
        originalElement: PsiElement,
        kotlinOriginKind: JvmDeclarationOriginKind,
    ): PsiMethod? {
        val javaMethod = BlockingBridgeStubMethodBuilder(
            originMethod.manager,
            originMethod.language,
            originMethod.name,
            originalElement
        ).apply {
            this.containingClass = containingClass
            docComment = originMethod.docComment
            navigationElement = originMethod


            for (it in parameters) {
                addParameter(it)
            }

            if (generateAsStatic) {
                addModifier(PsiModifier.STATIC)
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

                    fun PsiType.coerceUnitToVoid(): PsiType {
                        return if (this.canonicalText == "kotlin.Unit") PsiType.VOID else this
                    }

                    when (val type = psiClassReferenceType.parameters.getOrNull(0) ?: return null) {
                        is PsiWildcardType -> { // ? super String
                            setMethodReturnType((type.bound ?: type).coerceUnitToVoid())
                        }
                        else -> {
                            setMethodReturnType(type.coerceUnitToVoid())
                        }
                    }
                }

            originMethod.annotations.forEach { annotation ->
                if (annotation.hasQualifiedName("kotlin.Deprecated")) deprecated = true
                if (returnType?.canonicalText == "void"
                    && annotation.hasQualifiedName(JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION.asString())
                ) return@forEach // ignore @NotNull for coerced returnType `void`

                addAnnotation(annotation)
            }

            setBody(JavaPsiFacade.getElementFactory(project).createCodeBlock())
        }
        val kotlinOrigin = originMethod.kotlinOrigin?.let { kotlinOrigin ->
            LightMemberOriginForDeclaration(kotlinOrigin, kotlinOriginKind) // // TODO: 2020/8/4
        }

        return BlockingBridgeStubMethod({ javaMethod }, kotlinOrigin, containingClass).apply {
        }
//            KtLightMethodImpl.create(it, kotlinOrigin, containingClass).apply {
//                this.returnType
//            }
    }

    val overloads = originMethod.kotlinOrigin.safeAs<KtNamedFunction>()?.valueParameters // last is Continuation
        ?.jvmOverloads(originMethod.parameterList) ?: return emptyList()

    if (overloads.isEmpty()) return emptyList()

    val baseMethodParameters = overloads.first()

    val baseMethod =
        generateImpl(baseMethodParameters, originMethod, JvmDeclarationOriginKind.BRIDGE) ?: return emptyList()

    return overloads.mapNotNull {
        if (it === baseMethodParameters) {
            baseMethod
        } else {
            generateImpl(it, baseMethod, JvmDeclarationOriginKind.JVM_OVERLOADS)
        }
    }
}

private fun List<KtParameter>.jvmOverloads(parameterList: PsiParameterList): List<List<PsiParameter>>? {
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
    if (defaultValueCount == 0) return listOf(this.map { findPsiParameter(it.name) ?: return null })

    val result = mutableListOf<List<PsiParameter>>()
    for (count in defaultValueCount downTo 0) {
        result += takeNDefaults(count).map { findPsiParameter(it.name) ?: return null }
    }
    return result
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
