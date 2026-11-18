package com.anjo.filebrowser.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.anjo.filebrowser.ui.common.AppMenu
import com.anjo.filebrowser.ui.common.ColumnsHeader
import com.anjo.filebrowser.ui.common.ColumnsRow
import com.anjo.filebrowser.ui.common.EmptyState
import com.anjo.filebrowser.ui.common.ErrorState
import com.anjo.filebrowser.ui.common.FOLDER_COLUMN
import com.anjo.filebrowser.ui.common.FileName
import com.anjo.filebrowser.ui.common.Loadable
import com.anjo.filebrowser.ui.common.QuotaBar
import com.anjo.filebrowser.ui.common.SIZE_COLUMN
import com.anjo.filebrowser.ui.common.SkeletonList
import com.anjo.filebrowser.ui.common.SnackbarEffect
import com.anjo.filebrowser.ui.common.TIME_COLUMN
import com.anjo.filebrowser.ui.common.dividedItems
import com.anjo.filebrowser.ui.common.fitsColumns
import com.anjo.filebrowser.ui.common.folderLabel
import com.anjo.filebrowser.ui.common.formatSize
import com.anjo.filebrowser.ui.common.formatTime
import com.anjo.filebrowser.ui.files.MenuItem
import com.anjo.filebrowser.ui.files.WorkIndicator
import com.anjo.filebrowser.ui.model.TrashItem
import com.anjo.filebrowser.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

private const val DIALOG_NAME_LINES = 3

internal sealed interface Purge {
    data object All : Purge

    data class One(
        val item: TrashItem,
    ) : Purge
}

@Composable
fun TrashScreen(viewModel: TrashViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.refresh() }
    SnackbarEffect(viewModel.snackbar, snackbarHost)
    TrashContent(
        state = state,
        snackbarHost = snackbarHost,
        onRefresh = viewModel::refresh,
        onRestore = viewModel::restore,
        onDeleteForever = viewModel::deleteForever,
        onEmptyTrash = viewModel::emptyTrash,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrashContent(
    state: TrashUiState,
    snackbarHost: SnackbarHostState,
    onRefresh: () -> Unit,
    onRestore: (TrashItem) -> Unit,
    onDeleteForever: (TrashItem) -> Unit,
    onEmptyTrash: () -> Unit,
) {
    var purge by remember { mutableStateOf<Purge?>(null) }
    val items = (state.items as? Loadable.Ready)?.value.orEmpty()

    BoxWithConstraints {
        val wide = fitsColumns()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Trash", Modifier.semantics { heading() }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    actions = {
                        if (items.isNotEmpty() && wide) {
                            TextButton(
                                onClick = { purge = Purge.All },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) { Text("Empty trash") }
                        } else if (items.isNotEmpty()) {
                            IconButton(onClick = { purge = Purge.All }) {
                                Icon(Icons.Outlined.DeleteSweep, contentDescription = "Empty trash", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                        }
                        AppMenu()
                    },
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
                Text(
                    "Deleted items stay here for 30 days. They count toward your storage until they are deleted forever.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s).widthIn(max = Spacing.content),
                )
                when (val loaded = state.items) {
                    Loadable.Loading -> {
                        SkeletonList()
                    }

                    is Loadable.Failed -> {
                        ErrorState(loaded.message, onRefresh)
                    }

                    is Loadable.Ready -> {
                        if (loaded.value.isEmpty()) {
                            EmptyState(Icons.Outlined.Delete, "Trash is empty", "Files and folders you delete appear here.")
                        } else {
                            if (wide) {
                                ColumnsHeader(listOf("Original location" to FOLDER_COLUMN, "Size" to SIZE_COLUMN, "Deleted" to TIME_COLUMN))
                            }
                            LazyColumn(Modifier.fillMaxSize()) {
                                dividedItems(loaded.value, key = { it.id }) { item ->
                                    TrashRow(item, wide, onRestore = { onRestore(item) }, onPurge = { purge = Purge.One(item) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    purge?.let { request ->
        PurgeDialog(
            request = request,
            count = items.size,
            onConfirm = {
                purge = null
                when (request) {
                    Purge.All -> onEmptyTrash()
                    is Purge.One -> onDeleteForever(request.item)
                }
            },
            onDismiss = { purge = null },
        )
    }
}

@Composable
private fun TrashRow(
    item: TrashItem,
    wide: Boolean,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val size = if (item.directory) null else formatSize(item.size)
    val leading: @Composable () -> Unit = {
        Icon(
            if (item.directory) Icons.Outlined.Folder else Icons.Outlined.Description,
            contentDescription = if (item.directory) "Folder" else "File",
        )
    }
    val trailing: @Composable () -> Unit = {
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Actions for ${item.name}")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                MenuItem("Restore", Icons.Outlined.RestoreFromTrash, Modifier.semantics { contentDescription = "Restore ${item.name}" }) {
                    menu = false
                    onRestore()
                }
                MenuItem(
                    "Delete forever",
                    Icons.Outlined.DeleteForever,
                    Modifier.semantics { contentDescription = "Delete ${item.name} forever" },
                ) {
                    menu = false
                    onPurge()
                }
            }
        }
    }
    if (wide) {
        ColumnsRow(
            item.name,
            listOf(folderLabel(item.parent) to FOLDER_COLUMN, size.orEmpty() to SIZE_COLUMN, formatTime(item.deletedAt) to TIME_COLUMN),
            leading,
            trailing,
            Modifier.semantics(mergeDescendants = true) {},
        )
    } else {
        ListItem(
            headlineContent = { FileName(item.name, maxLines = 2) },
            supportingContent = {
                Text(listOfNotNull("From ${folderLabel(item.parent)}", size, "Deleted ${formatTime(item.deletedAt)}").joinToString(" · "))
            },
            leadingContent = leading,
            trailingContent = trailing,
        )
    }
}

internal fun emptyTrashText(count: Int): String =
    "${if (count == 1) "1 item" else "All $count items"} will be deleted forever. This cannot be undone."

@Composable
internal fun PurgeDialog(
    request: Purge,
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, confirm) =
        when (request) {
            Purge.All -> "Empty trash?" to "Empty trash"
            is Purge.One -> "Delete forever?" to "Delete forever"
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                when (request) {
                    Purge.All -> {
                        Text(emptyTrashText(count))
                    }

                    is Purge.One -> {
                        Text(
                            request.item.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = DIALOG_NAME_LINES,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text("It cannot be restored afterwards.")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep") } },
    )
}
