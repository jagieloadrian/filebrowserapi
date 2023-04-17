package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.CannotRealizeException
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
                "${generateDefaultPath()}/$path"
            } else generateDefaultPath()
        } ?: run {
            generateDefaultPath()
        }
        return calculatePath
    }

    fun validPath(path: String?) {
        if (path.isNullOrBlank()) {
            return
        } else {
            isExistPath(path)
        }
    }

    fun isExistPath(path: String) {
        val extractFile = File("${generateDefaultPath()}/$path")
        if (!extractFile.exists()) {
            throw CannotRealizeException("Path: $path to create file or directory is not exist")
        }
    }

    public fun generateDefaultPath(): String {
        return properties.baseDiskPath + baseDirectory
    }
}