package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.QuotaExceededException
import com.anjo.filebrowserapi.model.Quota
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

@Service
class QuotaManager(
    private val modifierUtils: PathModifierUtils,
    private val properties: AppConfiguration,
) {
    fun quota(): Quota = Quota(used(), properties.quota.toBytes())

    fun reserve(bytes: Long) {
        val used = used()
        val limit = properties.quota.toBytes()
        if (used + bytes > limit) throw QuotaExceededException("Quota exceeded: $used of $limit bytes used, $bytes more requested")
        val free = Files.getFileStore(Path.of(properties.path)).usableSpace
        if (free - bytes < properties.minFreeSpace.toBytes()) throw QuotaExceededException("Not enough free disk space")
    }

    // ponytail: walks the user's files, trash and versions on every write; keep a running total in the DB if trees grow large
    private fun used(): Long {
        val owner = modifierUtils.currentUser()
        return listOf(
            modifierUtils.resolve().toPath(),
            modifierUtils.storeDirectory(TRASH_FOLDER, owner),
            modifierUtils.storeDirectory(VERSIONS_FOLDER, owner),
        ).sumOf(::diskUsage)
    }
}

fun diskUsage(path: Path): Long = Files.walk(path).use { paths -> paths.filter(Files::isRegularFile).mapToLong(Files::size).sum() }
