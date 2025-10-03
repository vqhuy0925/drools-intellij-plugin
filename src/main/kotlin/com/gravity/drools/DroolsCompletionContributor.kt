package com.gravity.drools.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class DroolsCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                val kws = listOf("package","import","global","function","query","rule","when","then","end","salience","no-loop","agenda-group","dialect")
                result.addAllElements(kws.map { LookupElementBuilder.create(it) })
            }
        })
    }
}
