package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.OperationType
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path

private const val BASE_FOLDER = "filebrowser"

@Service
class PathModifierUtils(
    private val properties: AppConfiguration,
) {
    private val baseDirectory: Path = Path.of(properties.path, BASE_FOLDER).toAbsolutePath().normalize()

    init {
        val baseFolder = baseDirectory.toFile()
        if (!baseFolder.exists()) {
            baseFolder.mkdir()
        }
    }

    fun resolve(vararg segments: String?): File {
        val target =
            segments
                .filterNotNull()
                .filter { it.isNotBlank() }
                .fold(baseDirectory) { parent, segment -> parent.resolve(segment.trimStart('/')) }
                .normalize()
        if (!target.startsWith(baseDirectory)) throw BadInputException("Path is outside the base directory")
        return target.toFile()
    }

    fun resolveEntry(vararg segments: String?): File {
        val target = resolve(*segments)
        if (target.toPath() == baseDirectory) throw BadInputException("The base directory cannot be changed")
        return target
    }

    fun mergePath(path: String?): String = resolve(path).path

    fun validPath(
        path: String?,
        operationType: OperationType,
    ) {
        if (!path.isNullOrBlank()) isExistPath(path, operationType)
    }

    private fun isExistPath(
        path: String,
        operationType: OperationType,
    ) {
        val extractFile = resolve(path)
        if (!extractFile.exists()) {
            throw NotFoundException("Path: $path to ${operationType.description} file or directory is not exist")
        }
    }

    fun extractMidPath(sourcePath: String): List<String> = sourcePath.split("/").dropLast(1)

    fun generatePathToRename(
        extractMidPath: List<String>,
        newName: String,
    ): String = resolveEntry(*extractMidPath.toTypedArray(), newName).path

    fun generateDefaultPath(): String = baseDirectory.toString()

    fun formatPath(path: String): String = if (path.first() == '/') path.drop(1) else path
}
