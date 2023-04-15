package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.constants.Constant
import org.springframework.stereotype.Service

@Service
class FileManagerService(
    private val properties: AppConfiguration
) {

    fun uploadFile(){}

    private fun generateDefaultPath(): String {
        return properties.baseDiskPath + Constant.BASE_DIRECTORY
    }
}