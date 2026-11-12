package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.CannotRealizeException
import com.anjo.filebrowserapi.model.OperationType
import com.anjo.filebrowserapi.utils.formatResponsePath
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.io.File

@Service
class DirectoryManager(
    private val modifierUtils: PathModifierUtils,
) {
    private val logger = KotlinLogging.logger {}
    private val defaultPath = modifierUtils.generateDefaultPath()

    fun showDirectoriesTree(path: String?): List<String> {
        val mergePath = modifierUtils.mergePath(path)

        return File(mergePath)
            .walk()
            .filter { file ->
                file.isDirectory
            }.map { file ->
                formatResponsePath(file.path, defaultPath)
            }.drop(1)
            .sorted()
            .toList()
    }

    fun addDirectory(
        directoryName: String,
        path: String?,
    ): String {
        modifierUtils.validPath(path, OperationType.CREATE)
        val directory = modifierUtils.resolveEntry(path, directoryName)

        if (directory.mkdir()) {
            return formatResponsePath(directory.path, defaultPath)
        } else {
            throw CannotRealizeException(
                "Directory with name: $directoryName in path: ${
                    formatResponsePath(
                        directory.path,
                        defaultPath,
                    )
                } exist",
            )
        }
    }

    fun addDirectoriesRecursively(pathMultiDirectories: String): String {
        val directories = modifierUtils.resolveEntry(pathMultiDirectories)
        directories.mkdirs()
        return formatResponsePath(directories.path, defaultPath)
    }

    fun renameDirectory(
        newName: String,
        path: String,
    ): String {
        val validPath = modifierUtils.formatPath(path)
        val extractMidPath = modifierUtils.extractMidPath(validPath)

        val sourceDir = modifierUtils.resolveEntry(path)
        val targetPath = modifierUtils.generatePathToRename(extractMidPath, newName)
        val targetDir = File(targetPath)

        if (sourceDir.renameTo(targetDir)) {
            checkIfSuccess(targetDir)
            return formatResponsePath(targetDir.path, defaultPath)
        } else {
            throw CannotRealizeException("Cannot rename not existing directory")
        }
    }

    private fun checkIfSuccess(targetDir: File) {
        if (!targetDir.isDirectory) throw CannotRealizeException("Exception during operation")
    }

    fun deleteDirectory(path: String) {
        val file = modifierUtils.resolveEntry(path)

        if (isEmptyDirectory(file)) {
            file.delete()
        } else {
            throw CannotRealizeException("Cannot remove not empty directory")
        }
    }

    private fun isEmptyDirectory(file: File): Boolean {
        if (file.isDirectory) {
            return file.list()?.isEmpty() ?: false
        }
        return false
    }
}
