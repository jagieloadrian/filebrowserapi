package com.anjo.filebrowser.ui.trash

import com.anjo.filebrowser.ui.common.Loadable
import com.anjo.filebrowser.ui.model.Quota
import com.anjo.filebrowser.ui.model.TrashItem

data class TrashUiState(
    val items: Loadable<List<TrashItem>> = Loadable.Loading,
    val quota: Quota? = null,
    val working: Int = 0,
)
