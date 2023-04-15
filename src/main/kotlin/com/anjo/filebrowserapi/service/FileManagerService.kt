package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.constants.Constant
import com.anjo.filebrowserapi.exception.CannotRealizeException
import com.anjo.filebrowserapi.utils.StringUtils.Companion.formatResponsePath
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Service
class FileManagerService(
    private val properties: AppConfiguration
) {

    fun uploadFile(uploadFile: MultipartFile, path: String?) :String{
        validPath(path)
        val fileName = uploadFile.originalFilename!!
        val mergedPath = "${mergePath(path)}/$fileName"
        val targetLocation = Path.of(mergedPath)

        Files.copy(uploadFile.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

        return formatResponsePath(mergedPath, generateDefaultPath())
    }


    private fun validPath(path: String?) {
        if (path.isNullOrBlank()) {
            return
        } else {
            isExistPath(path)
        }
    }

    private fun isExistPath(path: String) {
        val extractFile = File("${generateDefaultPath()}/$path")
        if (!extractFile.exists()) {
            throw CannotRealizeException("Path: $path to create file is not exist")
        }
    }
    private fun mergePath(path: String?): String {
        val calculatePath = path?.let {
            if (it.isNotBlank()) {
                "${generateDefaultPath()}/$path"
            } else generateDefaultPath()
        } ?: run {
            generateDefaultPath()
        }
        return calculatePath
    }
    private fun generateDefaultPath(): String {
        return properties.baseDiskPath + Constant.BASE_DIRECTORY
    }
}