package com.gravity.drools.lang

import com.gravity.drools.DroolsFileType
import com.gravity.drools.DroolsLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class DroolsPsiFile(viewProvider: FileViewProvider)
    : PsiFileBase(viewProvider, DroolsLanguage) {
    override fun getFileType() = DroolsFileType
    override fun toString(): String = "Drools File"
}
