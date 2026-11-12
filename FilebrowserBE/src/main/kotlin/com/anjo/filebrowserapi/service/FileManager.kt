package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.CannotRealizeException
import com.anjo.filebrowserapi.model.OperationType.CREATE
import com.anjo.filebrowserapi.model.OperationType.DELETE
import com.anjo.filebrowserapi.model.OperationType.READ
import com.anjo.filebrowserapi.utils.formatResponsePath
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val RANDOM_NAME_LENGTH = 8

@Service
class FileManager(
    private val modifierUtils: PathModifierUtils,
) {
    private val defaultPath = modifierUtils.generateDefaultPath()
    private val formatDate = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")

    fun showFilesTree(path: String?): List<String> {
        val mergePath = modifierUtils.mergePath(path)
        return File(mergePath)
            .walk()
            .map { file -> formatResponsePath(file.path, defaultPath) }
            .drop(1)
            .sorted()
            .toList()
    }

    fun uploadFile(
        uploadFile: MultipartFile,
        path: String?,
    ): String {
        modifierUtils.validPath(path, CREATE)
        val fileName = validFileName(uploadFile.originalFilename)
        val target = modifierUtils.resolveEntry(path, fileName)

        Files.copy(uploadFile.inputStream, target.toPath(), StandardCopyOption.REPLACE_EXISTING)

        return formatResponsePath(target.path, defaultPath)
    }

    fun getFile(
        fileName: String,
        path: String?,
    ): ByteArray {
        modifierUtils.validPath(path, READ)
        return modifierUtils.resolveEntry(path, fileName).readBytes()
    }

    fun renameFile(
        newName: String,
        path: String?,
    ): String {
        val validPath = path?.let { modifierUtils.formatPath(it) } ?: defaultPath
        val extractMidPath = modifierUtils.extractMidPath(validPath)

        val sourceFile = modifierUtils.resolveEntry(path)
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
        if (!targetDir.isFile) throw CannotRealizeException("Exception during operation")
    }

    fun deleteFile(
        fileName: String,
        path: String?,
    ) {
        modifierUtils.validPath(path, DELETE)
        val file = modifierUtils.resolveEntry(path, fileName)
        if (!file.delete()) {
            throw BadInputException("Cannot remove non existing file")
        }
    }

    private fun validFileName(filename: String?): String =
        if (filename.isNullOrEmpty()) {
            val randomString = UUID.randomUUID().toString().take(RANDOM_NAME_LENGTH)
            "$randomString-${formatDate.format(LocalDateTime.now())}.txt"
        } else {
            filename
        }
}
