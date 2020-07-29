package net.mamoe.kjbb.ide

import com.google.auto.service.AutoService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

@AutoService(PsiReferenceProvider::class)
class JvmBlockingBridgePsiReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        return arrayOf()
    }
}