package com.gravity.drools.lang

import com.gravity.drools.DroolsLanguage
import com.gravity.drools.lexer.DroolsLexer
import com.gravity.drools.psi.DroolsTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class DroolsParserDefinition : ParserDefinition {
    companion object {
        val FILE = IFileElementType(DroolsLanguage)
        val WHITE_SPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
        val COMMENTS: TokenSet = TokenSet.create(DroolsTypes.LINE_COMMENT, DroolsTypes.BLOCK_COMMENT)
        val STRINGS: TokenSet = TokenSet.create(DroolsTypes.STRING_LITERAL)
    }

    override fun createLexer(project: Project?): Lexer = object : FlexAdapter(DroolsLexer(null)) {}
    override fun createParser(project: Project?): PsiParser = com.gravity.drools.parser.DroolsParser()

    override fun getFileNodeType(): IFileElementType = FILE
    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = STRINGS

    override fun createElement(node: ASTNode) = DroolsTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = DroolsPsiFile(viewProvider)
}
