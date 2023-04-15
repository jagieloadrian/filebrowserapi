package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.constants.Constant.BASE_DIRECTORY
import com.anjo.filebrowserapi.exception.CannotRealizeException
import com.anjo.filebrowserapi.utils.StringUtils.Companion.formatDirectoryPath
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import java.io.File


@Service
@EnableConfigurationProperties(AppConfiguration::class)
class DirectoryManagerService(
    private val properties: AppConfiguration
) {

    fun showDirectoriesTree(path: String?): List<String> {
        val mergePath = mergePath(path)

        return File(mergePath).walk().filter { file ->
            file.isDirectory
        }
            .map { file ->
                formatDirectoryPath(file.path, generateDefaultPath())
            }
            .drop(1)
            .toList()
    }

    fun addDirectory(directoryName: String, path: String?): String {
        validPath(path)
        val mergePath = "${mergePath(path)}/$directoryName"
        val directory = File(mergePath)

        if (directory.mkdir()) {
            return formatDirectoryPath(directory.path, generateDefaultPath())
        } else {
            throw CannotRealizeException(
                "Directory with name: $directoryName in path: ${
                    formatDirectoryPath(
                        directory.path,
                        generateDefaultPath()
                    )
                } exist"
            )
        }
    }

    fun addDirectoriesRecursively(pathMultiDirectories: String): String {
        val directories = File("${generateDefaultPath()}/$pathMultiDirectories")
        directories.mkdirs()
        return formatDirectoryPath(directories.path, generateDefaultPath())
    }

    fun renameDirectory(newName: String, path: String): String {
        val validPath = if (path.first().equals("/")) path.drop(1) else path
        val extractMidPath = validPath.split("/").dropLast(1)

        val sourceDir = File("${generateDefaultPath()}/$path")

        val joinMidPath: String
        val targetPath: String

        if (extractMidPath.isNotEmpty()) {
            joinMidPath = extractMidPath.joinToString(separator = "/")
            targetPath = "${generateDefaultPath()}/$joinMidPath/$newName"
        } else {
            targetPath = "${generateDefaultPath()}/$newName"
        }

        val targetDir = File(targetPath)

        if (sourceDir.renameTo(targetDir)) {
            return formatDirectoryPath(targetDir.path, generateDefaultPath())
        } else {
            throw CannotRealizeException("Cannot rename not existing directory")
        }
    }

    fun deleteDirectory(path: String) {
        val generatePath = "${generateDefaultPath()}/$path"
        val file = File(generatePath)

        if (isEmptyDirectory(file)) {
            file.delete()
        } else throw CannotRealizeException("Cannot remove not empty directory")
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

    private fun isEmptyDirectory(file: File): Boolean {
        if (file.isDirectory) {
            return file.list()?.isEmpty() ?: false
        }
        return false
    }

    private fun generateDefaultPath(): String {
        return properties.baseDiskPath + BASE_DIRECTORY
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
            throw CannotRealizeException("Path: $path to create directory is not exist")
        }
    }
}