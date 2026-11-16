package com.anjo.filebrowser.ui.platform

import com.anjo.filebrowser.ui.model.PickedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual suspend fun pickFile(): PickedFile? {
    val dialog = FileDialog(null as Frame?, "Upload file", FileDialog.LOAD)
    dialog.isVisible = true
    val file = dialog.files.firstOrNull() ?: return null
    return withContext(Dispatchers.IO) { PickedFile(file.name, file.readBytes()) }
}

actual suspend fun saveFile(
    name: String,
    content: suspend () -> ByteArray,
) {
    val dialog = FileDialog(null as Frame?, "Save file", FileDialog.SAVE)
    dialog.file = name
    dialog.isVisible = true
    val directory = dialog.directory ?: return
    val target = dialog.file ?: return
    val bytes = content()
    withContext(Dispatchers.IO) { File(directory, target).writeBytes(bytes) }
}
