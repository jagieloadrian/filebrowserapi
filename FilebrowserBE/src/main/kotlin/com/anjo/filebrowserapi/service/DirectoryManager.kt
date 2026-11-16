package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.AlreadyExistsException
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotEmptyException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.OperationType
import com.anjo.filebrowserapi.repository.MetadataRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files

@Service
class DirectoryManager(
    private val modifierUtils: PathModifierUtils,
    private val metadata: MetadataRepository,
    private val trash: TrashManager,
) {
    private val logger = KotlinLogging.logger {}

    fun showDirectoriesTree(path: String?): List<String> {
        val mergePath = modifierUtils.mergePath(path)

        return File(mergePath)
            .walk()
            .filter { file ->
                file.isDirectory
            }.map { file ->
                modifierUtils.relativePath(file)
            }.drop(1)
            .sorted()
            .toList()
    }

    fun addDirectory(
        directoryName: String,
        path: String?,
    ): String {
        modifierUtils.validPath(path, OperationType.CREATE)
        val directory = modifierUtils.resolveName(path, directoryName)

        if (directory.mkdir()) {
            return modifierUtils.relativePath(directory)
        } else {
            throw AlreadyExistsException(
                "Directory with name: $directoryName in path: ${modifierUtils.relativePath(directory)} exist",
            )
        }
    }

    fun addDirectoriesRecursively(pathMultiDirectories: String): String {
        val directories = modifierUtils.resolveEntry(pathMultiDirectories)
        directories.mkdirs()
        return modifierUtils.relativePath(directories)
    }

    fun renameDirectory(
        newName: String,
        path: String,
    ): String {
        val validPath = modifierUtils.formatPath(path)
        val extractMidPath = modifierUtils.extractMidPath(validPath)

        val sourceDir = modifierUtils.resolveEntry(path)
        val targetDir = File(modifierUtils.generatePathToRename(extractMidPath, newName))
        requireDirectory(sourceDir, "Cannot rename not existing directory")
        if (targetDir.exists()) throw AlreadyExistsException("Directory $newName already exists")

        Files.move(sourceDir.toPath(), targetDir.toPath())
        val target = modifierUtils.relativePath(targetDir)
        metadata.relocate(modifierUtils.currentUser(), modifierUtils.relativePath(sourceDir), target)
        return target
    }

    fun deleteDirectory(path: String) {
        val directory = modifierUtils.resolveEntry(path)
        requireDirectory(directory, "Directory $path does not exist")
        if (!directory.list().isNullOrEmpty()) throw NotEmptyException("Cannot remove not empty directory")
        trash.moveToTrash(directory)
    }

    fun deleteDirectoryRecursively(path: String) {
        val directory = modifierUtils.resolveEntry(path)
        requireDirectory(directory, "Directory $path does not exist")
        trash.moveToTrash(directory)
    }

    private fun requireDirectory(
        directory: File,
        missingMessage: String,
    ) {
        if (!directory.exists()) throw NotFoundException(missingMessage)
        if (!directory.isDirectory) {
            throw BadInputException("Path ${modifierUtils.relativePath(directory)} is not a directory")
        }
    }
}
