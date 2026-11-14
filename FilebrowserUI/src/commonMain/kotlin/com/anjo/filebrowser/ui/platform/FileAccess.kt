package com.anjo.filebrowser.ui.platform

import com.anjo.filebrowser.ui.model.PickedFile

expect suspend fun pickFile(): PickedFile?

expect suspend fun saveFile(
    name: String,
    content: suspend () -> ByteArray,
)
