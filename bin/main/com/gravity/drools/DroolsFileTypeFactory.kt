package com.gravity.drools

import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

@Suppress("DEPRECATION")
class DroolsFileTypeFactory : FileTypeFactory() {
    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(DroolsFileType, DroolsFileType.defaultExtension)
    }
}
