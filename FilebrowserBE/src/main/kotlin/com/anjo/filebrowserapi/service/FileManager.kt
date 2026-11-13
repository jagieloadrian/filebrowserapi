package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.AlreadyExistsException
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.OperationType.CREATE
import com.anjo.filebrowserapi.model.OperationType.DELETE
import com.anjo.filebrowserapi.model.OperationType.READ
import com.anjo.filebrowserapi.repository.MetadataRepository
import com.anjo.filebrowserapi.utils.formatResponsePath
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val RANDOM_NAME_LENGTH = 8

@Service
class FileManager(
    private val modifierUtils: PathModifierUtils,
    private val metadata: MetadataRepository,
) {
    private val defaultPath: String get() = modifierUtils.generateDefaultPath()
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
        originalName: String?,
        content: InputStream,
        path: String?,
        overwrite: Boolean = false,
    ): String {
        modifierUtils.validPath(path, CREATE)
        val fileName = validFileName(originalName)
        val target = modifierUtils.resolveName(path, fileName)
        if (target.isDirectory) throw AlreadyExistsException("Directory $fileName already exists")

        val options = if (overwrite) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
        try {
            Files.copy(content, target.toPath(), *options)
        } catch (_: FileAlreadyExistsException) {
            throw AlreadyExistsException("File $fileName already exists")
        }

        return formatResponsePath(target.path, defaultPath)
    }

    fun getFile(
        fileName: String,
        path: String?,
    ): File {
        modifierUtils.validPath(path, READ)
        val file = modifierUtils.resolveName(path, fileName)
        requireFile(file, "File $fileName does not exist")
        return file
    }

    fun renameFile(
        newName: String,
        path: String?,
    ): String {
        val validPath = path?.let { modifierUtils.formatPath(it) } ?: defaultPath
        val extractMidPath = modifierUtils.extractMidPath(validPath)

        val sourceFile = modifierUtils.resolveEntry(path)
        val targetFile = File(modifierUtils.generatePathToRename(extractMidPath, newName))
        requireFile(sourceFile, "Cannot rename not existing file")
        if (targetFile.exists()) throw AlreadyExistsException("File $newName already exists")

        Files.move(sourceFile.toPath(), targetFile.toPath())
        val target = formatResponsePath(targetFile.path, defaultPath)
        metadata.relocate(modifierUtils.currentUser(), formatResponsePath(sourceFile.path, defaultPath), target)
        return target
    }

    private fun requireFile(
        file: File,
        missingMessage: String,
    ) {
        if (!file.exists()) throw NotFoundException(missingMessage)
        if (!file.isFile) throw BadInputException("Path ${formatResponsePath(file.path, defaultPath)} is not a file")
    }

    fun deleteFile(
        fileName: String,
        path: String?,
    ) {
        modifierUtils.validPath(path, DELETE)
        val file = modifierUtils.resolveName(path, fileName)
        requireFile(file, "Cannot remove non existing file")
        Files.delete(file.toPath())
        metadata.forget(modifierUtils.currentUser(), formatResponsePath(file.path, defaultPath))
    }

    private fun validFileName(filename: String?): String =
        if (filename.isNullOrEmpty()) {
            val randomString = UUID.randomUUID().toString().take(RANDOM_NAME_LENGTH)
            "$randomString-${formatDate.format(LocalDateTime.now())}.txt"
        } else {
            filename
        }
}
