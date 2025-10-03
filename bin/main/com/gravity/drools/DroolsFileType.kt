package com.gravity.drools

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object DroolsFileType : LanguageFileType(DroolsLanguage) {
    override fun getName(): String = "Drools File"
    override fun getDescription(): String = "Drools rule file"
    override fun getDefaultExtension(): String = "drl"
    override fun getIcon(): Icon? = null // You can wire a custom icon here
}
