package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.CannotRealizeException
import com.anjo.filebrowserapi.model.OperationType
import org.intellij.lang.annotations.RegExp
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import java.io.File

@Service
@EnableConfigurationProperties(AppConfiguration::class)
class PathModifierUtils(
    private val properties: AppConfiguration
) {
    private val baseDirectory = "/filebrowser"

    fun mergePath(path: String?): String {
        val calculatePath = path?.let {
            if (it.isNotBlank()) {
                "${generateDefaultPath()}/${formatPath(path)}"
            } else generateDefaultPath()
        } ?: run {
            generateDefaultPath()
        }
        return calculatePath
    }

    fun validPath(path: String?, operationType: OperationType) {
        if (path.isNullOrBlank()) {
            return
        } else {
            isExistPath(path, operationType)
        }
    }

    fun isExistPath(path: String, operationType: OperationType) {
        val extractFile = File("${generateDefaultPath()}/$path")
        println(extractFile.absoluteFile)
        if (!extractFile.exists()) {
            throw CannotRealizeException("Path: $path to ${operationType.name} file or directory is not exist")
        }
    }

    fun generateDefaultPath(): String {
        return properties.baseDiskPath + baseDirectory
    }

    private fun formatPath(path: String) = if (path.first().equals(Regex("/"))) path.drop(1) else path
}