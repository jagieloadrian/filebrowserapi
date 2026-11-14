package com.anjo.filebrowser.ui

import com.anjo.filebrowser.ui.api.ApiException
import com.anjo.filebrowser.ui.api.ApiStatus
import com.anjo.filebrowser.ui.api.FilebrowserApi
import com.anjo.filebrowser.ui.model.Entry
import com.anjo.filebrowser.ui.model.PickedFile
import com.anjo.filebrowser.ui.model.Quota
import com.anjo.filebrowser.ui.model.TrashItem
import kotlin.time.Instant

class FakeFilebrowserApi : FilebrowserApi {
    val entries = mutableListOf<Entry>()
    val trashItems = mutableListOf<TrashItem>()
    val calls = mutableListOf<String>()
    val failures = mutableMapOf<String, ApiException>()
    var quota = Quota(used = 10, limit = 100)

    override suspend fun listEntries(
        path: String,
        name: String?,
    ): List<Entry> =
        record("list", "$path $name") {
            entries.filter { it.path.startsWith("$path/") && (name == null || it.name.contains(name, ignoreCase = true)) }
        }

    override suspend fun download(entry: Entry): ByteArray = record("download", entry.path) { ByteArray(0) }

    override suspend fun upload(
        directory: String,
        file: PickedFile,
        overwrite: Boolean,
    ) = record("upload", "$directory/${file.name} $overwrite") {
        if (!overwrite && entries.any { it.path == "$directory/${file.name}" }) {
            throw ApiException(ApiStatus.CONFLICT, "File ${file.name} already exists")
        }
    }

    override suspend fun createFolder(
        parent: String,
        name: String,
    ) = record("createFolder", "$parent/$name") {}

    override suspend fun rename(
        entry: Entry,
        newName: String,
    ) = record("rename", "${entry.path} $newName") {}

    override suspend fun delete(entry: Entry) = record("delete", entry.path) {}

    override suspend fun trash(): List<TrashItem> = record("trash", "") { trashItems.toList() }

    override suspend fun restore(item: TrashItem) = record("restore", item.id) {}

    override suspend fun deleteForever(item: TrashItem) = record("deleteForever", item.id) {}

    override suspend fun emptyTrash() = record("emptyTrash", "") {}

    override suspend fun quota(): Quota = record("quota", "") { quota }

    private fun <T> record(
        operation: String,
        arguments: String,
        block: () -> T,
    ): T {
        failures[operation]?.let { throw it }
        calls += "$operation $arguments".trim()
        return block()
    }
}

fun file(
    path: String,
    size: Long = 1,
) = Entry(path = path, directory = false, size = size, modified = Instant.parse("2026-11-13T10:00:00Z"))

fun folder(path: String) = Entry(path = path, directory = true, size = 0, modified = Instant.parse("2026-11-13T10:00:00Z"))

fun trashed(
    id: String,
    path: String,
    deletedAt: String = "2026-11-13T10:00:00Z",
) = TrashItem(id = id, path = path, directory = false, size = 1, deletedAt = Instant.parse(deletedAt))
