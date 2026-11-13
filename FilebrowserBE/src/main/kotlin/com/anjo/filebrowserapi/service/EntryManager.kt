package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.exception.AlreadyExistsException
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.Entry
import com.anjo.filebrowserapi.utils.formatResponsePath
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.time.Instant

@Service
class EntryManager(
    private val modifierUtils: PathModifierUtils,
) {
    fun listEntries(
        path: String?,
        name: String?,
        extension: String?,
    ): List<Entry> {
        val defaultPath = modifierUtils.generateDefaultPath()
        val directory = modifierUtils.resolve(path)
        if (!directory.isDirectory) throw NotFoundException("Directory ${formatResponsePath(directory.path, defaultPath)} does not exist")
        val wantedExtension = extension?.removePrefix(".")
        return directory
            .walk()
            .drop(1)
            .filter { name.isNullOrBlank() || it.name.contains(name, ignoreCase = true) }
            .filter { wantedExtension.isNullOrBlank() || (it.isFile && it.extension.equals(wantedExtension, ignoreCase = true)) }
            .map { it.toEntry(defaultPath) }
            .sortedBy { it.path }
            .toList()
    }

    fun copyEntry(
        source: String,
        target: String,
    ): String =
        transfer(source, target) { from, to ->
            from.copyRecursively(to)
        }

    fun moveEntry(
        source: String,
        target: String,
    ): String =
        transfer(source, target) { from, to ->
            Files.move(from.toPath(), to.toPath())
        }

    private fun transfer(
        source: String,
        target: String,
        action: (File, File) -> Unit,
    ): String {
        val from = modifierUtils.resolveEntry(source)
        val to = modifierUtils.resolveEntry(target)
        if (!from.exists()) throw NotFoundException("$source does not exist")
        if (to.exists()) throw AlreadyExistsException("$target already exists")
        if (to.toPath().startsWith(from.toPath())) throw BadInputException("Cannot put $source inside itself")
        if (to.parentFile?.isDirectory != true) throw NotFoundException("Target directory of $target does not exist")
        action(from, to)
        return formatResponsePath(to.path, modifierUtils.generateDefaultPath())
    }

    private fun File.toEntry(defaultPath: String) =
        Entry(
            path = formatResponsePath(path, defaultPath),
            directory = isDirectory,
            size = if (isFile) length() else 0,
            modified = Instant.ofEpochMilli(lastModified()),
            contentType = if (isFile) MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM).toString() else null,
        )
}
