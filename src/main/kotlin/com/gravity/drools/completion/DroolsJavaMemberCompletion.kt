package com.gravity.drools.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import java.util.Locale
import kotlin.math.max

class DroolsJavaMemberCompletion : CompletionContributor(), DumbAware {
    private val log = Logger.getInstance(DroolsJavaMemberCompletion::class.java)

    init {
        // Global registration in plugin.xml; self-filter by file type
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), MemberProvider(log))
    }

    private class MemberProvider(private val log: Logger) : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(p: CompletionParameters, c: ProcessingContext, r: CompletionResultSet) {
            val file = p.position.containingFile ?: return
            val fileType: FileType = file.fileType
            if (fileType.name != "Drools File") {
                return // don't act outside Drools files
            }

            val project = file.project
            val editor = p.editor ?: return
            val doc: Document = editor.document
            val text = doc.charsSequence
            val caret = p.offset

            // Try to find a '.' before caret (tolerate dummy id)
            val dotIdx = findDotBeforeCaret(text, caret)
            if (dotIdx != null) {
                // Receiver just before '.'
                var i = dotIdx - 1
                while (i >= 0 && text[i].isWhitespace()) i--
                val end = i
                if (end < 0) return
                while (i >= 0 && (Character.isJavaIdentifierPart(text[i]) || text[i] == '$')) i--
                val start = i + 1
                if (start > end) return
                val receiver = text.subSequence(start, end + 1).toString()

                // Classic DRL/global/import-camel
                inferReceiverType(receiver, file, project)?.let { klass ->
                    collectMembers(klass).forEach { r.addElement(it.toLookup()) }
                    return
                }

                // Path DSL: this.
                if (receiver == "this") {
                    for (fqn in listImports(file.text)) {
                        val simple = fqn.substringAfterLast('.')
                        r.addElement(LookupElementBuilder.create(simple.lowerCamel()).withTypeText(simple, true))
                    }
                    return
                }

                // Path DSL: foo. where foo == lowerCamel(SimpleName from imports)
                listImports(file.text).firstOrNull { fqn ->
                    fqn.substringAfterLast('.').lowerCamel() == receiver
                }?.let { matched ->
                    resolveClass(project, matched)?.let { klass ->
                        collectMembers(klass).forEach { r.addElement(it.toLookup()) }
                    }
                    return
                }

                // If we got here, we didn't know the receiver type
                log.info("Drools completion: no type for receiver '$receiver' at ${file.name}")
                return
            }

            // No dot found: if immediately left token is 'this', still propose import-derived props
            val leftToken = leftTokenText(text, caret)
            if (leftToken == "this") {
                for (fqn in listImports(file.text)) {
                    val simple = fqn.substringAfterLast('.')
                    r.addElement(LookupElementBuilder.create(simple.lowerCamel()).withTypeText(simple, true))
                }
                log.info("Drools completion: offered this.{props} without dot at ${file.name}")
            } else {
                log.info("Drools completion: no dot and left token != this (token='$leftToken') at ${file.name}")
            }
        }
    }
}

/* ---------- helpers ---------- */

// Find a '.' to the left of caret, allowing IntelliJ's dummy identifier and whitespace after the dot.
private fun findDotBeforeCaret(text: CharSequence, caret: Int): Int? {
    val start = max(0, caret - 128)
    for (idx in (caret - 1) downTo start) {
        if (text[idx] == '.') {
            var ok = true
            var j = idx + 1
            while (j < caret) {
                val ch = text[j]
                if (!ch.isWhitespace() && !Character.isJavaIdentifierPart(ch)) {
                    ok = false; break
                }
                j++
            }
            if (ok) return idx
        }
    }
    return null
}

// token immediately left of caret
private fun leftTokenText(text: CharSequence, caret: Int): String? {
    var i = caret - 1
    while (i >= 0 && text[i].isWhitespace()) i--
    if (i < 0) return null
    var end = i
    while (i >= 0 && Character.isJavaIdentifierPart(text[i])) i--
    val start = i + 1
    return if (start <= end) text.subSequence(start, end + 1).toString() else null
}

private fun String.lowerCamel(): String =
    if (isNotEmpty() && this[0].isUpperCase()) this.replaceFirstChar { it.lowercase(Locale.getDefault()) } else this

private fun listImports(fileText: String): List<String> =
    Regex("""(?m)^\s*import\s+([A-Za-z_][\w\.]*)\s*;?\s*$""")
        .findAll(fileText).map { it.groupValues[1] }.toList()

private fun inferReceiverType(symbol: String, file: PsiFile, project: Project): PsiClass? {
    val txt = file.text
    // $x : com.example.Person(...)
    Regex("""\b\$?\Q$symbol\E\s*:\s*([A-Za-z_][\w\.]*)\s*\(""")
        .find(txt)?.groupValues?.getOrNull(1)?.let { return resolveClass(project, it) }
    // global com.example.Service myService
    Regex("""\bglobal\s+([A-Za-z_][\w\.]*)\s+\Q$symbol\E\b""")
        .find(txt)?.groupValues?.getOrNull(1)?.let { return resolveClass(project, it) }
    // import-based camel mapping: foo == lowerCamel(SimpleName)
    listImports(txt).firstOrNull { it.substringAfterLast('.').lowerCamel() == symbol }
        ?.let { return resolveClass(project, it) }
    return null
}

private fun resolveClass(project: Project, fqn: String): PsiClass? =
    JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))

private data class MemberItem(val lookup: String, val owner: String, val tail: String, val fqnToImport: String?)
private fun MemberItem.toLookup(): LookupElement =
    LookupElementBuilder.create(lookup).withTypeText(owner, true).withTailText(tail, true)
        .withInsertHandler(AutoImportInsertHandler(fqnToImport))

private fun collectMembers(klass: PsiClass): List<MemberItem> {
    val owner = klass.qualifiedName ?: klass.name ?: "?"
    val out = mutableListOf<MemberItem>()
    // fields
    for (f in klass.allFields) {
        val typeText = f.type.presentableText
        out += MemberItem(f.name, owner, ": $typeText", f.type.canonicalText?.takeIf { it.contains('.') })
    }
    // zero-arg getters
    for (m in klass.allMethods) {
        if (m.parameterList.parametersCount == 0) {
            val n = m.name
            if (n.startsWith("get") || n.startsWith("is")) {
                val ret = m.returnType?.presentableText ?: "void"
                out += MemberItem("$n()", owner, ": $ret",
                    m.returnType?.canonicalText?.takeIf { it?.contains('.') == true })
            }
        }
    }
    return out
}

private class AutoImportInsertHandler(private val fqnToImport: String?) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val fqn = fqnToImport
        if (fqn.isNullOrBlank() || '.' !in fqn) return
        WriteCommandAction.runWriteCommandAction(context.project) {
            ensureImportPresent(context.file, context.document, fqn)
        }
    }
}

private fun ensureImportPresent(file: PsiFile, document: Document, fqn: String) {
    val t = document.text
    if (Regex("""(?m)^\s*import\s+\Q$fqn\E\s*;?\s*$""").containsMatchIn(t)) return
    val pkg = Regex("""(?m)^\s*package\s+.*$""").find(t)
    val insert = pkg?.range?.last?.plus(1) ?: 0
    val needsNL = insert != 0 && (insert >= t.length || t[insert] != '\n')
    val line = (if (needsNL) "\n" else "") + "import $fqn\n"
    document.insertString(insert, line)
    PsiDocumentManager.getInstance(file.project).commitDocument(document)
}
