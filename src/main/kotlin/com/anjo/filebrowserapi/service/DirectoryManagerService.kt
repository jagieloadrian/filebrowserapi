package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.CannotRealizeException
import com.anjo.filebrowserapi.utils.StringUtils.Companion.formatResponsePath
import org.springframework.stereotype.Service
import java.io.File


@Service
class DirectoryManagerService(
    private val modifierUtils: PathModifierUtils
) {
    private val defaultPath = modifierUtils.generateDefaultPath()

    fun showDirectoriesTree(path: String?): List<String> {
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

    fun addDirectory(directoryName: String, path: String?): String {
        modifierUtils.validPath(path)
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

    fun addDirectoriesRecursively(pathMultiDirectories: String): String {
        val directories = File("${defaultPath}/$pathMultiDirectories")
        directories.mkdirs()
        return formatResponsePath(directories.path, defaultPath)
    }

    fun renameDirectory(newName: String, path: String): String {
        val validPath = if (path.first().equals("/")) path.drop(1) else path
        val extractMidPath = validPath.split("/").dropLast(1)

        val sourceDir = File("${defaultPath}/$path")

        val joinMidPath: String
        val targetPath: String

        if (extractMidPath.isNotEmpty()) {
            joinMidPath = extractMidPath.joinToString(separator = "/")
            targetPath = "${defaultPath}/$joinMidPath/$newName"
        } else {
            targetPath = "${defaultPath}/$newName"
        }

        val targetDir = File(targetPath)

        if (sourceDir.renameTo(targetDir)) {
            return formatResponsePath(targetDir.path, defaultPath)
        } else {
            throw CannotRealizeException("Cannot rename not existing directory")
        }
    }

    fun deleteDirectory(path: String) {
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