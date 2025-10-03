package com.gravity.drools.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/**
 * Intentionally fires EVERYWHERE (no language filter) to prove completion pipeline is active.
 * Remove after debugging.
 */
class DroolsSmokeCompletion : CompletionContributor(), DumbAware {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(), // no language constraint on purpose
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(p: CompletionParameters, c: ProcessingContext, r: CompletionResultSet) {
                    r.addElement(LookupElementBuilder.create("🔧 drools-completion-live"))
                }
            }
        )
    }
}
