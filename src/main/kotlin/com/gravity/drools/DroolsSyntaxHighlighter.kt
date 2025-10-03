package com.gravity.drools

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.gravity.drools.psi.DroolsTypes
import com.gravity.drools.lexer.DroolsLexer
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

private val KEYWORD = TextAttributesKey.createTextAttributesKey(
    "DROOLS_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD
)
private val STRING = TextAttributesKey.createTextAttributesKey(
    "DROOLS_STRING", DefaultLanguageHighlighterColors.STRING
)
private val NUMBER = TextAttributesKey.createTextAttributesKey(
    "DROOLS_NUMBER", DefaultLanguageHighlighterColors.NUMBER
)
private val COMMENT = TextAttributesKey.createTextAttributesKey(
    "DROOLS_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT
)
private val BAD = TextAttributesKey.createTextAttributesKey(
    "DROOLS_BAD_CHAR", HighlighterColors.BAD_CHARACTER
)

class DroolsSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer(): Lexer = object : FlexAdapter(DroolsLexer(null)) {}

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = when (tokenType) {
        DroolsTypes.KW_PACKAGE, DroolsTypes.KW_IMPORT, DroolsTypes.KW_GLOBAL,
        DroolsTypes.KW_FUNCTION, DroolsTypes.KW_QUERY, DroolsTypes.KW_RULE,
        DroolsTypes.KW_WHEN, DroolsTypes.KW_THEN, DroolsTypes.KW_END,
        DroolsTypes.KW_SALIENCE, DroolsTypes.KW_NO_LOOP, DroolsTypes.KW_AGENDA_GROUP,
        DroolsTypes.KW_DIALECT -> arrayOf(KEYWORD)

        DroolsTypes.STRING_LITERAL -> arrayOf(STRING)
        DroolsTypes.INT_LITERAL    -> arrayOf(NUMBER)
        DroolsTypes.LINE_COMMENT,
        DroolsTypes.BLOCK_COMMENT  -> arrayOf(COMMENT)

        TokenType.BAD_CHARACTER    -> arrayOf(BAD)

        else -> emptyArray()
    }
}

class DroolsSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        DroolsSyntaxHighlighter()
}
