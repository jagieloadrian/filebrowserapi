package com.anjo.filebrowser.ui.model

import androidx.compose.ui.graphics.ImageBitmap
import com.anjo.filebrowser.ui.common.Loadable

data class FilePreview(
    val entry: Entry,
    val content: Loadable<PreviewContent> = Loadable.Loading,
)

sealed interface PreviewContent {
    data class Text(
        val lines: List<PreviewLine>,
        val shownBytes: Long?,
    ) : PreviewContent

    data class Image(
        val bitmap: ImageBitmap,
    ) : PreviewContent

    data class TooLarge(
        val maxBytes: Long,
    ) : PreviewContent

    data class TooManyPixels(
        val width: Int,
        val height: Int,
    ) : PreviewContent

    data object Unreadable : PreviewContent
}

data class PreviewLine(
    val text: String,
    val continued: Boolean = false,
)
