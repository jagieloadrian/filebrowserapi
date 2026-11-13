package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.FileVersion
import com.anjo.filebrowserapi.repository.VersionRepository
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

internal const val VERSIONS_FOLDER = "filebrowser-versions"
private const val KEPT_VERSIONS = 10

@Service
class VersionManager(
    private val modifierUtils: PathModifierUtils,
    private val versionRepository: VersionRepository,
) {
    fun keep(file: File) {
        val owner = modifierUtils.currentUser()
        val version =
            FileVersion(UUID.randomUUID(), modifierUtils.relativePath(file), file.length(), Instant.now().truncatedTo(ChronoUnit.MILLIS))
        versionRepository.insert(owner, version)
        runCatching { Files.move(file.toPath(), versionPath(owner, version.id)) }
            .onFailure { versionRepository.delete(owner, version.id) }
            .getOrThrow()
        versionRepository.list(owner, version.path).drop(KEPT_VERSIONS).forEach { delete(owner, it.id) }
    }

    fun list(path: String): List<FileVersion> {
        val file = modifierUtils.resolveEntry(path)
        return versionRepository.list(modifierUtils.currentUser(), modifierUtils.relativePath(file))
    }

    fun restore(id: String): String {
        val owner = modifierUtils.currentUser()
        val uuid = runCatching { UUID.fromString(id) }.getOrElse { throw BadInputException("Invalid version id '$id'") }
        val version = versionRepository.find(owner, uuid) ?: throw NotFoundException("Version $id does not exist")
        val target = modifierUtils.resolveEntry(version.path)
        if (target.isDirectory) throw BadInputException("${version.path} is a directory now")
        if (target.isFile) keep(target)
        Files.createDirectories(target.toPath().parent)
        Files.move(versionPath(owner, uuid), target.toPath())
        versionRepository.delete(owner, uuid)
        return version.path
    }

    fun deleteFiles(
        owner: String,
        path: String,
    ) {
        versionRepository.idsUnder(owner, path).forEach { Files.deleteIfExists(versionPath(owner, it)) }
    }

    private fun delete(
        owner: String,
        id: UUID,
    ) {
        Files.deleteIfExists(versionPath(owner, id))
        versionRepository.delete(owner, id)
    }

    private fun versionPath(
        owner: String,
        id: UUID,
    ): Path = modifierUtils.storeDirectory(VERSIONS_FOLDER, owner).resolve(id.toString())
}
