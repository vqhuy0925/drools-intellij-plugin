package com.gravity.drools.navigation

import com.gravity.drools.DroolsLanguage
import com.gravity.drools.psi.DroolsTypes
import com.intellij.lang.ASTNode
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import java.util.Locale

/**
 * Provides references for DRL identifiers so Ctrl+Click / Go To Declaration jumps to Java classes.
 * Heuristics:
 *  - If identifier == lowerCamel(imported SimpleName), link to that imported class.
 *  - Also works when identifier appears in a property chain like "this.identityVerification".
 */
class DroolsReferenceContributor : PsiReferenceContributor(), DumbAware {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement().withLanguage(DroolsLanguage),
            object : PsiReferenceProvider() {
                override fun acceptsTarget(target: PsiElement): Boolean = target is PsiClass

                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val node: ASTNode = element.node ?: return PsiReference.EMPTY_ARRAY
                    if (node.elementType != DroolsTypes.IDENTIFIER) return PsiReference.EMPTY_ARRAY

                    val file = element.containingFile ?: return PsiReference.EMPTY_ARRAY
                    if (file.fileType.name != "Drools File") return PsiReference.EMPTY_ARRAY

                    val name = element.text
                    if (name.isNullOrBlank()) return PsiReference.EMPTY_ARRAY

                    // Map lowerCamel property name to imported SimpleName
                    val fqn = matchImportByLowerCamel(name, file.text) ?: return PsiReference.EMPTY_ARRAY

                    return arrayOf(object : PsiReferenceBase<PsiElement>(element, TextRange(0, name.length), /*soft*/ true) {
                        override fun resolve(): PsiElement? = resolveClass(element.project, fqn)
                        override fun getVariants(): Array<Any> = emptyArray()
                    })
                }
            }
        )
    }
}

/* ---------- helpers (same style you used in completion) ---------- */

private fun matchImportByLowerCamel(identifier: String, fileText: String): String? {
    val imports = Regex("""(?m)^\s*import\s+([A-Za-z_][\w\.]*)\s*;?\s*$""")
        .findAll(fileText)
        .map { it.groupValues[1] }
        .toList()

    val id = identifier.trim()
    for (fqn in imports) {
        val simple = fqn.substringAfterLast('.')
        val lowerCamel = simple.toLowerCamel()
        if (lowerCamel == id) return fqn
    }
    return null
}

private fun String.toLowerCamel(): String =
    if (isNotEmpty() && first().isUpperCase()) replaceFirstChar { it.lowercase(Locale.getDefault()) } else this

private fun resolveClass(project: Project, fqn: String): PsiClass? =
    JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
