package com.gravity.drools

import com.intellij.lexer.EmptyLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class DroolsSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer(): Lexer = EmptyLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        // This stub returns plain text highlighting.
        return emptyArray()
    }
}

class DroolsSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return DroolsSyntaxHighlighter()
    }
}
