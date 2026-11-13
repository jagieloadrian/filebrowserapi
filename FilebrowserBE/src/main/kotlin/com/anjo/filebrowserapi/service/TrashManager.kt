package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.AlreadyExistsException
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.TrashItem
import com.anjo.filebrowserapi.repository.MetadataRepository
import com.anjo.filebrowserapi.repository.TrashRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

internal const val TRASH_FOLDER = "filebrowser-trash"
private const val TRASH_KEY = "trash:"
private val RETENTION: Duration = Duration.ofDays(30)

@Service
class TrashManager(
    private val modifierUtils: PathModifierUtils,
    private val metadata: MetadataRepository,
    private val trashRepository: TrashRepository,
    private val versions: VersionManager,
) {
    private val logger = KotlinLogging.logger {}

    fun moveToTrash(entry: File) {
        val owner = modifierUtils.currentUser()
        val item =
            TrashItem(
                id = UUID.randomUUID(),
                path = modifierUtils.relativePath(entry),
                directory = entry.isDirectory,
                size = entry.walk().filter { it.isFile }.sumOf { it.length() },
                deletedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
            )
        trashRepository.insert(owner, item)
        runCatching { Files.move(entry.toPath(), trashPath(owner, item.id)) }
            .onFailure { trashRepository.delete(owner, item.id) }
            .getOrThrow()
        metadata.relocate(owner, item.path, key(item.id))
    }

    fun list(): List<TrashItem> = trashRepository.list(modifierUtils.currentUser())

    fun restore(id: String): String {
        val owner = modifierUtils.currentUser()
        val item = find(owner, id)
        val target = modifierUtils.resolveEntry(item.path).toPath()
        if (Files.exists(target)) throw AlreadyExistsException("${item.path} already exists")
        Files.createDirectories(target.parent)
        Files.move(trashPath(owner, item.id), target)
        metadata.relocate(owner, key(item.id), item.path)
        trashRepository.delete(owner, item.id)
        return item.path
    }

    fun purge(id: String) {
        val owner = modifierUtils.currentUser()
        purge(owner, find(owner, id).id)
    }

    fun empty() {
        val owner = modifierUtils.currentUser()
        trashRepository.list(owner).forEach { purge(owner, it.id) }
    }

    @Scheduled(cron = "0 0 3 * * *")
    fun purgeExpired() {
        val expired = trashRepository.findExpired(Instant.now().minus(RETENTION))
        expired.forEach { (owner, id) -> purge(owner, id) }
        logger.info { "Purged ${expired.size} trash items older than ${RETENTION.toDays()} days" }
    }

    private fun purge(
        owner: String,
        id: UUID,
    ) {
        trashPath(owner, id).toFile().deleteRecursively()
        versions.deleteFiles(owner, key(id))
        metadata.forget(owner, key(id))
        trashRepository.delete(owner, id)
    }

    private fun find(
        owner: String,
        id: String,
    ): TrashItem {
        val uuid = runCatching { UUID.fromString(id) }.getOrElse { throw BadInputException("Invalid trash id '$id'") }
        return trashRepository.find(owner, uuid) ?: throw NotFoundException("Trash item $id does not exist")
    }

    private fun trashPath(
        owner: String,
        id: UUID,
    ): Path = modifierUtils.storeDirectory(TRASH_FOLDER, owner).resolve(id.toString())

    private fun key(id: UUID) = "$TRASH_KEY$id"
}
