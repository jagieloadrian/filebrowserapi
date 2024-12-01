package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.CannotRealizeException
import com.anjo.filebrowserapi.model.OperationType
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import java.io.File

@Service
@EnableConfigurationProperties(AppConfiguration::class)
class PathModifierUtils(
    private val properties: AppConfiguration
) {
    private val baseDirectory = "/filebrowser"

    init {
        val baseFolder = File("${properties.baseDiskPath}$baseDirectory")
        if(!baseFolder.exists()){
            baseFolder.mkdir()
        }
    }

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
        when (path.isNullOrBlank()) {
            true -> return
            false -> isExistPath(path, operationType)
        }
    }

    fun isExistPath(path: String, operationType: OperationType) {
        val extractFile = File("${generateDefaultPath()}/$path")
        if (!extractFile.exists()) {
            throw CannotRealizeException("Path: $path to ${operationType.description} file or directory is not exist")
        }
    }

    fun generateDefaultPath(): String {
        return properties.baseDiskPath + baseDirectory
    }

    fun formatPath(path: String): String {
        return if (path.first() == '/') path.drop(1) else path
    }
}