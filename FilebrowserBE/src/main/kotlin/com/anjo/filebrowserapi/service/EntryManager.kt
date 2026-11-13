package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.AlreadyExistsException
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.Entry
import com.anjo.filebrowserapi.model.EntryMeta
import com.anjo.filebrowserapi.repository.MetadataRepository
import com.anjo.filebrowserapi.utils.formatResponsePath
import com.anjo.filebrowserapi.validation.normalizeDescription
import com.anjo.filebrowserapi.validation.normalizeTags
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.time.Instant

@Service
class EntryManager(
    private val modifierUtils: PathModifierUtils,
    private val metadata: MetadataRepository,
) {
    fun listEntries(
        path: String?,
        name: String?,
        extension: String?,
        tag: String? = null,
    ): List<Entry> {
        val defaultPath = modifierUtils.generateDefaultPath()
        val directory = modifierUtils.resolve(path)
        if (!directory.isDirectory) throw NotFoundException("Directory ${formatResponsePath(directory.path, defaultPath)} does not exist")
        val wantedExtension = extension?.removePrefix(".")
        val wantedTag = tag?.lowercase()
        val metas = metadata.findUnder(modifierUtils.currentUser(), formatResponsePath(directory.path, defaultPath))
        return directory
            .walk()
            .drop(1)
            .filter { name.isNullOrBlank() || it.name.contains(name, ignoreCase = true) }
            .filter { wantedExtension.isNullOrBlank() || (it.isFile && it.extension.equals(wantedExtension, ignoreCase = true)) }
            .map { it.toEntry(defaultPath, metas) }
            .filter { wantedTag.isNullOrBlank() || wantedTag in it.tags }
            .sortedBy { it.path }
            .toList()
    }

    fun copyEntry(
        source: String,
        target: String,
    ): String =
        transfer(source, target) { from, to, fromKey, toKey ->
            from.copyRecursively(to)
            metadata.copy(modifierUtils.currentUser(), fromKey, toKey)
        }

    fun moveEntry(
        source: String,
        target: String,
    ): String =
        transfer(source, target) { from, to, fromKey, toKey ->
            Files.move(from.toPath(), to.toPath())
            metadata.relocate(modifierUtils.currentUser(), fromKey, toKey)
        }

    private fun transfer(
        source: String,
        target: String,
        action: (File, File, String, String) -> Unit,
    ): String {
        val from = modifierUtils.resolveEntry(source)
        val to = modifierUtils.resolveEntry(target)
        if (!from.exists()) throw NotFoundException("$source does not exist")
        if (to.exists()) throw AlreadyExistsException("$target already exists")
        if (to.toPath().startsWith(from.toPath())) throw BadInputException("Cannot put $source inside itself")
        if (to.parentFile?.isDirectory != true) throw NotFoundException("Target directory of $target does not exist")
        val defaultPath = modifierUtils.generateDefaultPath()
        val toKey = formatResponsePath(to.path, defaultPath)
        action(from, to, formatResponsePath(from.path, defaultPath), toKey)
        return toKey
    }

    fun updateMeta(
        path: String,
        description: String?,
        tags: List<String>,
    ): Entry {
        val file = modifierUtils.resolveEntry(path)
        if (!file.exists()) throw NotFoundException("$path does not exist")
        val defaultPath = modifierUtils.generateDefaultPath()
        val meta = EntryMeta(formatResponsePath(file.path, defaultPath), normalizeDescription(description), normalizeTags(tags))
        metadata.save(modifierUtils.currentUser(), meta.path, meta.description, meta.tags)
        return file.toEntry(defaultPath, mapOf(meta.path to meta))
    }

    private fun File.toEntry(
        defaultPath: String,
        metas: Map<String, EntryMeta>,
    ): Entry {
        val key = formatResponsePath(path, defaultPath)
        return Entry(
            path = key,
            directory = isDirectory,
            size = if (isFile) length() else 0,
            modified = Instant.ofEpochMilli(lastModified()),
            contentType = if (isFile) MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM).toString() else null,
            description = metas[key]?.description,
            tags = metas[key]?.tags.orEmpty(),
        )
    }
}
