package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.model.OperationType.*
import com.anjo.filebrowserapi.utils.CommonUtils.Companion.EIGHT
import com.anjo.filebrowserapi.utils.CommonUtils.Companion.formatResponsePath
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
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

    fun showFilesTree(path: String?): List<String> {
        val mergePath = modifierUtils.mergePath(path)
        return File(mergePath).walk()
            .map { file -> formatResponsePath(file.path, defaultPath) }
            .drop(1)
            .toList()
    }

    fun uploadFile(uploadFile: MultipartFile, path: String?): String {
        modifierUtils.validPath(path, CREATE)
        val fileName = validFileName(uploadFile.originalFilename)
        val mergedPath = "${modifierUtils.mergePath(path)}/$fileName"
        val targetLocation = Path.of(mergedPath)

        Files.copy(uploadFile.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

        return formatResponsePath(mergedPath, defaultPath)
    }

    fun getFile(fileName: String, path: String?): ByteArray {
        modifierUtils.validPath(path, READ)
        val mergedPath = "${modifierUtils.mergePath(path)}/$fileName"
        val targetFile = File(mergedPath)
        return targetFile.readBytes()
    }

    fun deleteFile(fileName: String, path: String?) {
        modifierUtils.validPath(path, DELETE)
        val mergedPath = "${modifierUtils.mergePath(path)}/$fileName"

        val file = File(mergedPath)
        if(!file.delete()) {
            throw BadInputException("Cannot remove non existing file")
        }
    }


    fun validFileName(filename: String?): String {
        return if (filename.isNullOrEmpty()) {
            val randomString = UUID.randomUUID().toString().take(EIGHT)
            "$randomString-${formatDate.format(LocalDateTime.now())}.txt"
        } else {
            filename
        }
    }
}