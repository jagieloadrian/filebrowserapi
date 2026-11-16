package com.anjo.filebrowser.ui.files

import com.anjo.filebrowser.ui.common.Loadable
import com.anjo.filebrowser.ui.model.Entry
import com.anjo.filebrowser.ui.model.FilePreview
import com.anjo.filebrowser.ui.model.Quota

data class FilesUiState(
    val path: String = "",
    val query: String = "",
    val entries: Loadable<List<Entry>> = Loadable.Loading,
    val quota: Quota? = null,
    val working: Int = 0,
    val preview: FilePreview? = null,
)
