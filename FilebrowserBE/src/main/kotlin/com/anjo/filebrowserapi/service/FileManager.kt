package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.CannotRealizeException
import com.anjo.filebrowserapi.facade.FileManager
import com.anjo.filebrowserapi.model.OperationType.CREATE
import com.anjo.filebrowserapi.model.OperationType.DELETE
import com.anjo.filebrowserapi.model.OperationType.READ
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
import java.util.UUID

@Service
class FileManager(
        private val modifierUtils: PathModifierUtils
) : FileManager {
    private val defaultPath = modifierUtils.generateDefaultPath()
    private val formatDate = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")

    override fun showFilesTree(path: String?): List<String> {
        val mergePath = modifierUtils.mergePath(path)
        return File(mergePath).walk()
                .map { file -> formatResponsePath(file.path, defaultPath) }
                .drop(1)
                .toList()
    }

    override fun uploadFile(uploadFile: MultipartFile, path: String?): String {
        modifierUtils.validPath(path, CREATE)
        val fileName = validFileName(uploadFile.originalFilename)
        val mergedPath = "${modifierUtils.mergePath(path)}/$fileName"
        val targetLocation = Path.of(mergedPath)

        Files.copy(uploadFile.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

        return formatResponsePath(mergedPath, defaultPath)
    }

    override fun getFile(fileName: String, path: String?): ByteArray {
        modifierUtils.validPath(path, READ)
        val mergedPath = "${modifierUtils.mergePath(path)}/$fileName"
        val targetFile = File(mergedPath)
        return targetFile.readBytes()
    }

    override fun renameFile(newName: String, path: String?): String {
        val validPath = path?.let { modifierUtils.formatPath(it) } ?: defaultPath
        val extractMidPath = modifierUtils.extractMidPath(validPath)

        val sourceFile = File("${defaultPath}/$path")
        val targetPath = modifierUtils.generatePathToRename(extractMidPath, newName)
        val targetDir = File(targetPath)

        if (sourceFile.renameTo(targetDir)) {
            checkIfSuccess(targetDir)
            return formatResponsePath(targetDir.path, defaultPath)
        } else {
            throw CannotRealizeException("Cannot rename not existing directory")
        }
    }

    private fun checkIfSuccess(targetDir: File) {
        when (targetDir.exists() && !targetDir.isDirectory) {
            true  -> { return }
            false -> { throw CannotRealizeException("Exception during operation") }
        }
    }

    override fun deleteFile(fileName: String, path: String?) {
        modifierUtils.validPath(path, DELETE)
        val mergedPath = "${modifierUtils.mergePath(path)}/$fileName"

        val file = File(mergedPath)
        if (!file.delete()) {
            throw BadInputException("Cannot remove non existing file")
        }
    }

    private fun validFileName(filename: String?): String {
        return if (filename.isNullOrEmpty()) {
            val randomString = UUID.randomUUID().toString().take(EIGHT)
            "$randomString-${formatDate.format(LocalDateTime.now())}.txt"
        } else {
            filename
        }
    }
}