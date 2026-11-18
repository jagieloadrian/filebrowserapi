package com.anjo.filebrowser.ui.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.anjo.filebrowser.ui.common.AppMenu
import com.anjo.filebrowser.ui.common.ColumnsHeader
import com.anjo.filebrowser.ui.common.ColumnsRow
import com.anjo.filebrowser.ui.common.EmptyState
import com.anjo.filebrowser.ui.common.ErrorState
import com.anjo.filebrowser.ui.common.FOLDER_COLUMN
import com.anjo.filebrowser.ui.common.FileName
import com.anjo.filebrowser.ui.common.Loadable
import com.anjo.filebrowser.ui.common.NameDialog
import com.anjo.filebrowser.ui.common.QuotaBar
import com.anjo.filebrowser.ui.common.SIZE_COLUMN
import com.anjo.filebrowser.ui.common.SkeletonList
import com.anjo.filebrowser.ui.common.SnackbarEffect
import com.anjo.filebrowser.ui.common.TIME_COLUMN
import com.anjo.filebrowser.ui.common.TYPE_COLUMN
import com.anjo.filebrowser.ui.common.dividedItems
import com.anjo.filebrowser.ui.common.fileType
import com.anjo.filebrowser.ui.common.fitsColumns
import com.anjo.filebrowser.ui.common.folderLabel
import com.anjo.filebrowser.ui.common.formatSize
import com.anjo.filebrowser.ui.common.formatTime
import com.anjo.filebrowser.ui.model.Entry
import com.anjo.filebrowser.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

private val UPLOAD_BUTTON_CLEARANCE = 88.dp

private sealed interface NameRequest {
    data object NewFolder : NameRequest

    data class Rename(
        val entry: Entry,
    ) : NameRequest
}

@Composable
fun FilesScreen(viewModel: FilesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.refresh() }
    SnackbarEffect(viewModel.snackbar, snackbarHost)
    FilesContent(
        state = state,
        snackbarHost = snackbarHost,
        onRefresh = viewModel::refresh,
        onUpload = viewModel::uploadFromDevice,
        onSearch = viewModel::search,
        onOpen = viewModel::open,
        onPreview = viewModel::preview,
        onClosePreview = viewModel::closePreview,
        onDownload = viewModel::download,
        onCreateFolder = viewModel::createFolder,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilesContent(
    state: FilesUiState,
    snackbarHost: SnackbarHostState,
    onRefresh: () -> Unit,
    onUpload: () -> Unit,
    onSearch: (String) -> Unit,
    onOpen: (String) -> Unit,
    onPreview: (Entry) -> Unit,
    onClosePreview: () -> Unit,
    onDownload: (Entry) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRename: (Entry, String) -> Unit,
    onDelete: (Entry) -> Unit,
) {
    var nameRequest by remember { mutableStateOf<NameRequest?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Files", Modifier.semantics { heading() }) },
                actions = {
                    IconButton(onClick = { nameRequest = NameRequest.NewFolder }) {
                        Icon(Icons.Outlined.CreateNewFolder, contentDescription = "New folder")
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                    AppMenu()
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onUpload,
                icon = { Icon(Icons.Outlined.Upload, contentDescription = null) },
                text = { Text("Upload file") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            WorkIndicator(state.working > 0)
            QuotaBar(state.quota)
            SearchField(state.query, onSearch)
            Breadcrumbs(state.path, onOpen)
            when (val entries = state.entries) {
                Loadable.Loading -> {
                    SkeletonList()
                }

                is Loadable.Failed -> {
                    ErrorState(entries.message, onRefresh)
                }

                is Loadable.Ready -> {
                    if (entries.value.isEmpty()) {
                        NoEntries(state.query)
                    } else {
                        EntryList(
                            entries = entries.value,
                            searching = state.query.isNotBlank(),
                            onOpen = onOpen,
                            onPreview = onPreview,
                            onDownload = onDownload,
                            onRename = { nameRequest = NameRequest.Rename(it) },
                            onDelete = onDelete,
                        )
                    }
                }
            }
        }
    }

    state.preview?.let { preview ->
        PreviewDialog(
            preview,
            onClose = onClosePreview,
            onRetry = { onPreview(preview.entry) },
            onDownload = { onDownload(preview.entry) },
            onRename = {
                onClosePreview()
                nameRequest = NameRequest.Rename(preview.entry)
            },
            onDelete = {
                onClosePreview()
                onDelete(preview.entry)
            },
        )
    }

    nameRequest?.let { request ->
        val renamed = (request as? NameRequest.Rename)?.entry
        NameDialog(
            title = if (renamed == null) "New folder" else "Rename",
            confirmLabel = if (renamed == null) "Create folder" else "Rename",
            initial = renamed?.name.orEmpty(),
            onDismiss = { nameRequest = null },
            onConfirm = { name ->
                nameRequest = null
                if (renamed == null) onCreateFolder(name) else onRename(renamed, name)
            },
        )
    }
}

@Composable
internal fun WorkIndicator(working: Boolean) {
    if (working) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    } else {
        Spacer(Modifier.height(Spacing.xs))
    }
}

@Composable
private fun SearchField(
    query: String,
    onSearch: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onSearch,
        label = { Text("Search in this folder") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon =
            if (query.isEmpty()) {
                null
            } else {
                { IconButton(onClick = { onSearch("") }) { Icon(Icons.Outlined.Close, contentDescription = "Clear search") } }
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.padding(horizontal = Spacing.m).widthIn(max = Spacing.content).fillMaxWidth(),
    )
}

@Composable
private fun Breadcrumbs(
    path: String,
    onOpen: (String) -> Unit,
) {
    val parts = path.split('/').filter { it.isNotEmpty() }
    val crumbs = listOf("" to folderLabel("")) + parts.indices.map { i -> "/" + parts.take(i + 1).joinToString("/") to parts[i] }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.s, vertical = Spacing.xs)
            .heightIn(min = LocalMinimumInteractiveComponentSize.current),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        crumbs.forEachIndexed { index, (target, label) ->
            if (index > 0) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
            if (index == crumbs.lastIndex) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = Spacing.s).semantics { heading() },
                )
            } else {
                TextButton(onClick = { onOpen(target) }) { Text(label) }
            }
        }
    }
}

@Composable
private fun NoEntries(query: String) {
    if (query.isBlank()) {
        EmptyState(Icons.Outlined.FolderOpen, "This folder is empty", "Upload a file or create a folder to fill it.")
    } else {
        EmptyState(Icons.Outlined.SearchOff, "No matches for \"${query.trim()}\"", "Try a shorter part of the name.")
    }
}

@Composable
private fun EntryList(
    entries: List<Entry>,
    searching: Boolean,
    onOpen: (String) -> Unit,
    onPreview: (Entry) -> Unit,
    onDownload: (Entry) -> Unit,
    onRename: (Entry) -> Unit,
    onDelete: (Entry) -> Unit,
) {
    BoxWithConstraints {
        val wide = fitsColumns()
        Column {
            if (wide) {
                ColumnsHeader(
                    listOfNotNull(
                        ("Folder" to FOLDER_COLUMN).takeIf { searching },
                        "Type" to TYPE_COLUMN,
                        "Size" to SIZE_COLUMN,
                        "Modified" to TIME_COLUMN,
                    ),
                )
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = UPLOAD_BUTTON_CLEARANCE)) {
                dividedItems(entries, key = { it.path }) { entry ->
                    EntryRow(
                        entry = entry,
                        searching = searching,
                        wide = wide,
                        onOpen = { onOpen(entry.path) },
                        onPreview = { onPreview(entry) },
                        onDownload = { onDownload(entry) },
                        onRename = { onRename(entry) },
                        onDelete = { onDelete(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: Entry,
    searching: Boolean,
    wide: Boolean,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val previewable = previewKind(entry) != null
    val size = if (entry.directory) null else formatSize(entry.size)
    val leading: @Composable () -> Unit = {
        Icon(
            if (entry.directory) Icons.Outlined.Folder else Icons.Outlined.Description,
            contentDescription = if (entry.directory || wide) null else "File",
        )
    }
    val trailing: @Composable () -> Unit = {
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Actions for ${entry.name}")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (previewable) {
                    MenuItem("Preview", Icons.Outlined.Preview) {
                        menu = false
                        onPreview()
                    }
                }
                if (entry.directory) {
                    MenuItem("Open", Icons.Outlined.FolderOpen) {
                        menu = false
                        onOpen()
                    }
                } else {
                    MenuItem("Download", Icons.Outlined.Download) {
                        menu = false
                        onDownload()
                    }
                }
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
    val modifier =
        Modifier.clickable(
            onClickLabel =
                when {
                    entry.directory -> "Open folder"
                    previewable -> "Preview file"
                    else -> "Show actions"
                },
        ) {
            when {
                entry.directory -> onOpen()
                previewable -> onPreview()
                else -> menu = true
            }
        }
    if (wide) {
        ColumnsRow(
            entry.name,
            listOfNotNull(
                (folderLabel(entry.parent) to FOLDER_COLUMN).takeIf { searching },
                (if (entry.directory) "Folder" else fileType(entry.name)) to TYPE_COLUMN,
                size.orEmpty() to SIZE_COLUMN,
                formatTime(entry.modified) to TIME_COLUMN,
            ),
            leading,
            trailing,
            modifier,
        )
    } else {
        ListItem(
            headlineContent = { FileName(entry.name, maxLines = 2) },
            supportingContent = {
                val folder = if (searching) "In ${folderLabel(entry.parent)}" else null
                Text(listOfNotNull(folder, size ?: "Folder", "Modified ${formatTime(entry.modified)}").joinToString(" · "))
            },
            leadingContent = leading,
            trailingContent = trailing,
            modifier = modifier,
        )
    }
}

@Composable
internal fun MenuItem(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
        modifier = modifier,
    )
}
