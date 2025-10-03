package com.gravity.drools.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static com.gravity.drools.psi.DroolsTypes.*;

%%

%public
%class DroolsLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

WHITE_SPACE = [ \t\f\r\n]+
LINE_COMMENT = "//".* (\n|\r|\r\n)?
BLOCK_COMMENT = "/\\*" [^*]* (\\*+ [^/*] [^*]* )* \\*+ "/"

ID = [a-zA-Z_][a-zA-Z_0-9]*
INT = [0-9]+
STRING = \"([^\"\\]|\\.)*\"

%%

{WHITE_SPACE}     { return com.intellij.psi.TokenType.WHITE_SPACE; }
{LINE_COMMENT}    { return LINE_COMMENT; }
{BLOCK_COMMENT}   { return BLOCK_COMMENT; }

"package"         { return KW_PACKAGE; }
"import"          { return KW_IMPORT; }
"global"          { return KW_GLOBAL; }
"function"        { return KW_FUNCTION; }
"query"           { return KW_QUERY; }
"rule"            { return KW_RULE; }
"when"            { return KW_WHEN; }
"then"            { return KW_THEN; }
"end"             { return KW_END; }
"salience"        { return KW_SALIENCE; }
"no-loop"         { return KW_NO_LOOP; }
"agenda-group"    { return KW_AGENDA_GROUP; }
"dialect"         { return KW_DIALECT; }

"("               { return LPAREN; }
")"               { return RPAREN; }
"{"               { return LBRACE; }
"}"               { return RBRACE; }
"["               { return LBRACK; }
"]"               { return RBRACK; }
","               { return COMMA; }
";"               { return SEMI; }
":"               { return COLON; }
"="               { return EQ; }
"=="              { return EQEQ; }
"->"              { return ARROW; }
"."               { return DOT; }
"*"               { return STAR; }

{INT}             { return INT_LITERAL; }
{STRING}          { return STRING_LITERAL; }
{ID}              { return IDENTIFIER; }

.                 { return com.intellij.psi.TokenType.BAD_CHARACTER; }

