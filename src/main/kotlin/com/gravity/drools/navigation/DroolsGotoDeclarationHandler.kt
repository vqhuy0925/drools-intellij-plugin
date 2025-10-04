package com.gravity.drools.navigation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DroolsGotoDeclarationHandler : GotoDeclarationHandler, DumbAware {

    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor): Array<PsiElement>? {
        if (sourceElement == null) return null
        val file = sourceElement.containingFile ?: return null
        if (file.fileType.name != "Drools File") return null

        val project = sourceElement.project
        val text = editor.document.charsSequence

        // 1) Robust path-root detection around the caret
        val root = findNearestPathRootAroundCaret(text, offset)
        if (root != null) {
            resolveClassForPathRoot(project, root.token, file.text)?.let { return arrayOf(it) }
        }

        // 2) Member navigation: /Root[...]  this.<prop>
        var word = wordAt(text, offset)
        if (word != null && isAfterThisDot(text, word)) {
            val baseRoot = nearestEnclosingPathRoot(text, word.startOffset)
            if (baseRoot != null) {
                val baseClass = resolveClassForPathRoot(project, baseRoot, file.text)
                    ?: findBySimpleName(project, baseRoot.pascal())
                if (baseClass != null) {
                    val ident = text.subSequence(word.startOffset, word.endOffset).toString()
                    val member = resolveMember(baseClass, ident)
                    classOfMemberType(project, member)?.let { return arrayOf(it) }
                    if (member != null) return arrayOf(member)
                    return arrayOf(baseClass)
                }
            }
        }

        val clickedWord = wordAt(text, offset)
        if (clickedWord != null) {
            // Check if the word is preceded by a dot
            val dotIndex = findPrecedingDot(text, clickedWord.startOffset)
            if (dotIndex != -1) {
                // Find the class name identifier before the dot
                val classNameToken = findClassNameBeforeDot(text, dotIndex)
                if (classNameToken != null && classNameToken.token != "this") {
                    val methodName = text.subSequence(clickedWord.startOffset, clickedWord.endOffset).toString()

                    // Resolve the class using its name
                    val psiClass = resolveClassForPathRoot(project, classNameToken.token, file.text)
                    if (psiClass != null) {
                        // Find the method(s) by name within the resolved class
                        val methods = psiClass.findMethodsByName(methodName, true)
                        if (methods.isNotEmpty()) {
                            // Suppress the safe, but unchecked, cast warning
                            @Suppress("UNCHECKED_CAST")
                            return methods as Array<PsiElement>
                        }
                    }
                }
            }
        }

        // 3) Class name fallbacks (now acts as a final fallback)
        val identWord = word ?: wordAt(text, offset - 1)
        val ident = identWord?.let { text.subSequence(it.startOffset, it.endOffset).toString() }
        if (!ident.isNullOrBlank()) {
            matchImportByLowerCamel(ident, file.text)?.let { fqn ->
                resolveClass(project, fqn)?.let { return arrayOf(it) }
            }
            resolveViaImportsOrFqn(project, ident, file.text)?.let { return arrayOf(it) }
        }

        return null
    }

    override fun getActionText(context: com.intellij.openapi.actionSystem.DataContext): String? = null
}

// --- Data class for token information ---
private data class IdToken(val start: Int, val end: Int, val token: String)


/**
 * Finds the index of a preceding '.' character, skipping whitespace.
 * Returns -1 if no dot is found.
 */
private fun findPrecedingDot(text: CharSequence, wordStartOffset: Int): Int {
    var i = wordStartOffset - 1
    while (i >= 0 && text[i].isWhitespace()) {
        i--
    }
    return if (i >= 0 && text[i] == '.') i else -1
}

/**
 * Finds the full class name identifier immediately before a dot.
 */
private fun findClassNameBeforeDot(text: CharSequence, dotIndex: Int): IdToken? {
    var end = dotIndex - 1
    // Skip whitespace before the class name
    while (end >= 0 && text[end].isWhitespace()) {
        end--
    }
    if (end < 0) return null

    var start = end
    // Go to the beginning of the identifier
    while (start >= 0 && Character.isJavaIdentifierPart(text[start])) {
        start--
    }
    start++ // Adjust to the first character of the identifier

    return if (Character.isJavaIdentifierStart(text[start])) {
        IdToken(start, end + 1, text.subSequence(start, end + 1).toString())
    } else {
        null
    }
}

/**
 * Finds the nearest `/token` around the caret. This allows clicks on the slash,
 * any character of the token, or whitespace immediately after the token to all
 * resolve to the same path root.
 */
private fun findNearestPathRootAroundCaret(text: CharSequence, offset: Int): IdToken? {
    if (text.isEmpty()) return null

    // Probe several offsets for better accuracy: caret, caret-1, caret-2
    val probes = intArrayOf(offset, offset - 1, offset - 2).filter { it in text.indices }
    var best: IdToken? = null
    var bestDist = Int.MAX_VALUE

    for (probe in probes) {
        // Find an identifier token at the probe position
        val tok = pathTokenAtOffset(text, probe) ?: continue

        // Check for a slash immediately to the left of the token
        if (isImmediatelyPrecededBySlash(text, tok)) {
            val center = (tok.start + tok.end) / 2
            val dist = abs(probe - center)
            if (dist < bestDist) {
                best = tok
                bestDist = dist
            }
        }

        // Also check if the click was ON the slash itself
        if (probe > 0 && text[probe - 1] == '/') {
            val after = tokenRightOfSlash(text, probe - 1)
            if (after != null) {
                val center = (after.start + after.end) / 2
                val dist = abs(probe - center)
                if (dist < bestDist) {
                    best = after
                    bestDist = dist
                }
            }
        }
    }
    return best
}

/**
 * A more resilient function to find a Java identifier at a given offset.
 * It correctly handles clicks on adjacent whitespace or non-identifier characters
 * by searching nearby for a valid token.
 */
private fun pathTokenAtOffset(text: CharSequence, offset: Int): IdToken? {
    if (offset !in text.indices) return null

    var cursor = offset

    // If the character at the cursor isn't part of an identifier, search left
    // to find the end of a potential token.
    if (!Character.isJavaIdentifierPart(text[cursor])) {
        cursor--
        while (cursor >= 0 && !Character.isJavaIdentifierPart(text[cursor])) {
            // Stop searching if we cross a clear boundary like a slash
            if (text[cursor] == '/' || text[cursor] == '\n') break
            cursor--
        }
    }
    if (cursor < 0) return null

    // Now that we're inside a token, expand left and right to find its boundaries.
    var start = cursor
    while (start > 0 && Character.isJavaIdentifierPart(text[start - 1])) {
        start--
    }

    var end = cursor
    while (end < text.length - 1 && Character.isJavaIdentifierPart(text[end + 1])) {
        end++
    }
    end++ // Adjust to be exclusive

    return if (start < end) IdToken(start, end, text.subSequence(start, end).toString()) else null
}

/**
 * Finds the first identifier token that appears after a slash index.
 */
private fun tokenRightOfSlash(text: CharSequence, slashIdx: Int): IdToken? {
    var i = slashIdx + 1
    // Skip whitespace after the slash
    while (i < text.length && text[i].isWhitespace()) i++
    if (i >= text.length || !Character.isJavaIdentifierStart(text[i])) return null

    val start = i
    i++
    while (i < text.length && Character.isJavaIdentifierPart(text[i])) i++
    return IdToken(start, i, text.subSequence(start, i).toString())
}

private fun isImmediatelyPrecededBySlash(text: CharSequence, id: IdToken): Boolean {
    var i = id.start - 1
    while (i >= 0 && text[i].isWhitespace()) i--
    return i >= 0 && text[i] == '/'
}

private fun wordAt(text: CharSequence, offset: Int): TextRange? {
    if (offset < 0 || offset > text.length) return null
    var left = min(offset, text.length - 1)

    // Handle clicking right after a word
    if (left > 0 && !Character.isJavaIdentifierPart(text[left])) {
        left--
    }

    if (left < 0 || !Character.isJavaIdentifierPart(text[left])) return null

    var start = left
    while (start > 0 && Character.isJavaIdentifierPart(text[start - 1])) {
        start--
    }

    var end = left
    while (end < text.length - 1 && Character.isJavaIdentifierPart(text[end + 1])) {
        end++
    }

    return TextRange(start, end + 1)
}


private fun isAfterThisDot(text: CharSequence, word: TextRange): Boolean {
    var i = word.startOffset - 1
    while (i >= 0 && text[i].isWhitespace()) i--
    if (i < 0 || text[i] != '.') return false
    i--
    while (i >= 0 && text[i].isWhitespace()) i--
    return i >= 3 &&
            text[i] == 's' && text[i - 1] == 'i' && text[i - 2] == 'h' && text[i - 3] == 't' &&
            (i - 4 < 0 || !Character.isJavaIdentifierPart(text[i - 4]))
}

private fun nearestEnclosingPathRoot(text: CharSequence, fromOffset: Int): String? {
    val startScan = max(0, fromOffset - 2000)
    var i = fromOffset
    while (i >= startScan) {
        if (i >= 4 &&
            text[i - 4].equals('r', true) &&
            text[i - 3].equals('u', true) &&
            text[i - 2].equals('l', true) &&
            text[i - 1].equals('e', true) &&
            text[i].isWhitespace()
        ) break
        if (text[i] == '/') {
            var j = i + 1
            while (j < text.length && text[j].isWhitespace()) j++
            if (j < text.length && Character.isJavaIdentifierStart(text[j])) {
                val s = j
                j++
                while (j < text.length && Character.isJavaIdentifierPart(text[j])) j++
                return text.subSequence(s, j).toString()
            }
        }
        i--
    }
    return null
}

private fun resolveClassForPathRoot(project: Project, rootIdent: String, fileText: String): PsiClass? {
    val simple = rootIdent.pascal()
    listImports(fileText).firstOrNull { it.endsWith(".$simple") }?.let { fqn ->
        resolveClass(project, fqn)?.let { return it }
    }
    return findBySimpleName(project, simple)
}

private fun resolveMember(psiClass: PsiClass, prop: String): PsiElement? {
    psiClass.findFieldByName(prop, true)?.let { return it }
    val cap = prop.replaceFirstChar { it.uppercaseChar() }
    psiClass.findMethodsByName("get$cap", true).firstOrNull()?.let { return it }
    psiClass.findMethodsByName("is$cap", true).firstOrNull()?.let { return it }
    return null
}

private fun classOfMemberType(project: Project, member: PsiElement?): PsiClass? =
    when (member) {
        is PsiField -> member.type.canonicalText?.let { if ('.' in it) resolveClass(project, it) else findBySimpleName(project, it) }
        is PsiMethod -> member.returnType?.canonicalText?.let { if ('.' in it) resolveClass(project, it) else findBySimpleName(project, it) }
        else -> null
    }

private fun listImports(fileText: String): List<String> =
    Regex("""(?m)^\s*import\s+([A-Za-z_][\w\.]*)\s*;?\s*$""")
        .findAll(fileText).map { it.groupValues[1] }.toList()

private fun matchImportByLowerCamel(identifier: String, fileText: String): String? {
    val id = identifier.trim()
    for (fqn in listImports(fileText)) {
        val simple = fqn.substringAfterLast('.')
        if (simple.toLowerCamel() == id) return fqn
    }
    return null
}

private fun resolveViaImportsOrFqn(project: Project, ident: String, fileText: String): PsiElement? {
    listImports(fileText).firstOrNull { it.endsWith(".$ident") }?.let {
        resolveClass(project, it)?.let { cls -> return cls }
    }
    if ('.' in ident) resolveClass(project, ident)?.let { return it }
    return null
}

private fun String.toLowerCamel(): String =
    if (isNotEmpty() && first().isUpperCase()) replaceFirstChar { it.lowercase(Locale.getDefault()) } else this

private fun String.pascal(): String =
    if (isNotEmpty()) replaceFirstChar { it.titlecase(Locale.getDefault()) } else this

private fun resolveClass(project: Project, fqn: String): PsiClass? =
    JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))

private fun findBySimpleName(project: Project, simple: String): PsiClass? =
    JavaPsiFacade.getInstance(project).findClasses(simple, GlobalSearchScope.allScope(project)).firstOrNull()