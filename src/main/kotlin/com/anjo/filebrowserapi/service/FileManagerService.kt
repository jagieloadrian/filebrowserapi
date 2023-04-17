package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.utils.StringUtils.Companion.formatResponsePath
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class FileManagerService(
    private val modifierUtils: PathModifierUtils
) {
    private val defaultPath = modifierUtils.generateDefaultPath()
    private val formatDate = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")
    fun uploadFile(uploadFile: MultipartFile, path: String?): String {
        modifierUtils.validPath(path)
        val extension = uploadFile.resource.file.extension
        val fileName = validFileName(uploadFile.originalFilename, extension)
        val mergedPath = "${modifierUtils.mergePath(path)}/$fileName"
        val targetLocation = Path.of(mergedPath)

        Files.copy(uploadFile.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

        return formatResponsePath(mergedPath, defaultPath)
    }


    fun validFileName(filename:String?, extension:String):String{
        return if (filename.isNullOrEmpty()) {
            val randomString = UUID.randomUUID().toString().take(8)
            "$randomString-${formatDate.format(LocalDateTime.now())}.$extension"
        } else {
            filename
        }
    }
}