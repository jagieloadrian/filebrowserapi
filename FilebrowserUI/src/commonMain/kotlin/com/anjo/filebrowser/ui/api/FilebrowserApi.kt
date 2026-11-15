package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.ui.model.Entry
import com.anjo.filebrowser.ui.model.PickedFile
import com.anjo.filebrowser.ui.model.Quota
import com.anjo.filebrowser.ui.model.TrashItem

interface FilebrowserApi {
    suspend fun listEntries(
        path: String,
        name: String?,
    ): List<Entry>

    suspend fun download(
        entry: Entry,
        maxBytes: Long? = null,
    ): ByteArray

    suspend fun upload(
        directory: String,
        file: PickedFile,
        overwrite: Boolean,
    )

    suspend fun createFolder(
        parent: String,
        name: String,
    )

    suspend fun rename(
        entry: Entry,
        newName: String,
    )

    suspend fun delete(entry: Entry)

    suspend fun trash(): List<TrashItem>

    suspend fun restore(item: TrashItem)

    suspend fun deleteForever(item: TrashItem)

    suspend fun emptyTrash()

    suspend fun quota(): Quota
}
