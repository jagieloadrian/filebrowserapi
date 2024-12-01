package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.CannotRealizeException
import com.anjo.filebrowserapi.facade.DirectoryManager
import com.anjo.filebrowserapi.model.OperationType
import com.anjo.filebrowserapi.utils.CommonUtils.Companion.formatResponsePath
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.io.File


@Service
class DirectoryManager(
        private val modifierUtils: PathModifierUtils
) : DirectoryManager {
    private val logger = KotlinLogging.logger {}
    private val defaultPath = modifierUtils.generateDefaultPath()

    override fun showDirectoriesTree(path: String?): List<String> {
        val mergePath = modifierUtils.mergePath(path)

        return File(mergePath).walk().filter { file ->
            file.isDirectory
        }
                .map { file ->
                    formatResponsePath(file.path, defaultPath)
                }
                .drop(1)
                .toList()
    }

    override fun addDirectory(directoryName: String, path: String?): String {
        modifierUtils.validPath(path, OperationType.CREATE)
        val mergePath = "${modifierUtils.mergePath(path)}/$directoryName"
        val directory = File(mergePath)

        if (directory.mkdir()) {
            return formatResponsePath(directory.path, defaultPath)
        } else {
            throw CannotRealizeException(
                    "Directory with name: $directoryName in path: ${
                        formatResponsePath(
                                directory.path,
                                defaultPath
                        )
                    } exist"
            )
        }
    }

    override fun addDirectoriesRecursively(pathMultiDirectories: String): String {
        val directories = File("${defaultPath}/$pathMultiDirectories")
        directories.mkdirs()
        return formatResponsePath(directories.path, defaultPath)
    }

    override fun renameDirectory(newName: String, path: String): String {
        val validPath = modifierUtils.formatPath(path)
        val extractMidPath = modifierUtils.extractMidPath(validPath)

        val sourceDir = File("${defaultPath}/$path")
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
        when (targetDir.exists() && targetDir.isDirectory) {
            true  -> { return }
            false -> { throw CannotRealizeException("Exception during operation") }
        }
    }

    override fun deleteDirectory(path: String) {
        val generatePath = "${defaultPath}/$path"
        val file = File(generatePath)

        if (isEmptyDirectory(file)) {
            file.delete()
        } else throw CannotRealizeException("Cannot remove not empty directory")
    }

    private fun isEmptyDirectory(file: File): Boolean {
        if (file.isDirectory) {
            return file.list()?.isEmpty() ?: false
        }
        return false
    }
}