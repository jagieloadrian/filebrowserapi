package com.anjo.filebrowser.ui.files

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.WrapText
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anjo.filebrowser.ui.common.EmptyState
import com.anjo.filebrowser.ui.common.ErrorState
import com.anjo.filebrowser.ui.common.Loadable
import com.anjo.filebrowser.ui.common.formatSize
import com.anjo.filebrowser.ui.model.FilePreview
import com.anjo.filebrowser.ui.model.PreviewContent
import com.anjo.filebrowser.ui.model.PreviewLine
import com.anjo.filebrowser.ui.theme.Spacing

internal const val PREVIEW_LINE = "previewLine"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PreviewDialog(
    preview: FilePreview,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var wrap by rememberSaveable { mutableStateOf(true) }
    val content = (preview.content as? Loadable.Ready)?.value
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val compact = maxWidth < Spacing.wide
            Surface(
                if (compact) Modifier.fillMaxSize() else Modifier.fillMaxSize().padding(Spacing.xl),
                shape = if (compact) RectangleShape else MaterialTheme.shapes.extraLarge,
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text(preview.entry.name, Modifier.semantics { heading() }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Close preview") }
                        },
                        actions = {
                            if (content is PreviewContent.Text) {
                                IconToggleButton(checked = wrap, onCheckedChange = { wrap = it }) {
                                    Icon(Icons.AutoMirrored.Outlined.WrapText, contentDescription = "Wrap lines")
                                }
                            }
                            IconButton(onClick = onDownload) { Icon(Icons.Outlined.Download, contentDescription = "Download") }
                            MoreActions(onRename, onDelete)
                        },
                    )
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (val loaded = preview.content) {
                            Loadable.Loading -> {
                                CircularProgressIndicator(
                                    Modifier.align(Alignment.Center).semantics { contentDescription = "Loading preview" },
                                )
                            }

                            is Loadable.Failed -> {
                                ErrorState(loaded.message, onRetry)
                            }

                            is Loadable.Ready -> {
                                Content(loaded.value, preview, wrap, onDownload)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreActions(
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "More actions") }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            MenuItem("Rename", Icons.Outlined.DriveFileRenameOutline) {
                menu = false
                onRename()
            }
            MenuItem("Delete", Icons.Outlined.Delete) {
                menu = false
                onDelete()
            }
        }
    }
}

@Composable
private fun Content(
    content: PreviewContent,
    preview: FilePreview,
    wrap: Boolean,
    onDownload: () -> Unit,
) {
    val download: @Composable () -> Unit = { FilledTonalButton(onClick = onDownload) { Text("Download file") } }
    when (content) {
        is PreviewContent.Text -> {
            TextPreview(content, preview.entry.size, wrap)
        }

        is PreviewContent.Image -> {
            Image(
                content.bitmap,
                contentDescription = preview.entry.name,
                modifier = Modifier.fillMaxSize().padding(Spacing.m),
                contentScale = ContentScale.Inside,
            )
        }

        is PreviewContent.TooLarge -> {
            EmptyState(
                Icons.Outlined.FileDownload,
                "Too large to preview",
                "This file is ${formatSize(preview.entry.size)}. Previews show files up to ${formatSize(content.maxBytes)}. " +
                    "Download it to open it in another app.",
                download,
            )
        }

        is PreviewContent.TooManyPixels -> {
            EmptyState(
                Icons.Outlined.FileDownload,
                "Too large to preview",
                "This image is ${content.width} × ${content.height} pixels, more than the preview can show. " +
                    "Download it to open it in another app.",
                download,
            )
        }

        PreviewContent.Unreadable -> {
            EmptyState(
                Icons.Outlined.BrokenImage,
                "Cannot preview this file",
                "It may be damaged or in a format the preview cannot read. Download it to open it in another app.",
                download,
            )
        }
    }
}

@Composable
private fun TextPreview(
    content: PreviewContent.Text,
    size: Long,
    wrap: Boolean,
) {
    val style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    Column {
        content.shownBytes?.let {
            Text(
                "Showing the first ${formatSize(it)} of ${formatSize(size)}. Download the file to see all of it.",
                Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SelectionContainer(Modifier.weight(1f).fillMaxWidth()) {
            if (wrap) {
                Lines(content.lines, style, wrap = true, Modifier.fillMaxSize())
            } else {
                val measurer = rememberTextMeasurer()
                val longest =
                    remember(content.lines) {
                        content.lines
                            .maxByOrNull { it.text.length }
                            ?.text
                            .orEmpty()
                    }
                val width = remember(longest, style) { measurer.measure(longest, style, softWrap = false).size.width }
                Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                    Lines(
                        content.lines,
                        style,
                        wrap = false,
                        Modifier.fillMaxHeight().width(with(LocalDensity.current) { width.toDp() } + Spacing.m * 2),
                    )
                }
            }
        }
    }
}

@Composable
private fun Lines(
    lines: List<PreviewLine>,
    style: TextStyle,
    wrap: Boolean,
    modifier: Modifier,
) {
    LazyColumn(modifier, contentPadding = PaddingValues(Spacing.m)) {
        items(lines.size) { Text(lines[it].text, Modifier.testTag(PREVIEW_LINE), style = style, softWrap = wrap) }
    }
}
