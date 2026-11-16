package com.anjo.filebrowser.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.anjo.filebrowser.ui.common.AppMenu
import com.anjo.filebrowser.ui.common.ColumnsRow
import com.anjo.filebrowser.ui.common.EmptyState
import com.anjo.filebrowser.ui.common.ErrorState
import com.anjo.filebrowser.ui.common.FOLDER_COLUMN
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
import com.anjo.filebrowser.ui.files.WorkIndicator
import com.anjo.filebrowser.ui.model.TrashItem
import com.anjo.filebrowser.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

private sealed interface Purge {
    data object All : Purge

    data class One(
        val item: TrashItem,
    ) : Purge
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(viewModel: TrashViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var purge by remember { mutableStateOf<Purge?>(null) }
    LaunchedEffect(viewModel) { viewModel.refresh() }
    SnackbarEffect(viewModel.snackbar, snackbarHost)
    val items = (state.items as? Loadable.Ready)?.value.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash", Modifier.semantics { heading() }) },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(
                            onClick = { purge = Purge.All },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text("Empty trash") }
                    }
                    IconButton(onClick = viewModel::refresh) {
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
                    ErrorState(loaded.message, viewModel::refresh)
                }

                is Loadable.Ready -> {
                    if (loaded.value.isEmpty()) {
                        EmptyState(Icons.Outlined.Delete, "Trash is empty", "Files and folders you delete appear here.")
                    } else {
                        BoxWithConstraints {
                            val wide = fitsColumns()
                            LazyColumn(Modifier.fillMaxSize()) {
                                dividedItems(loaded.value, key = { it.id }) { item ->
                                    TrashRow(item, wide, onRestore = { viewModel.restore(item) }, onPurge = { purge = Purge.One(item) })
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
                    Purge.All -> viewModel.emptyTrash()
                    is Purge.One -> viewModel.deleteForever(request.item)
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
    val folder = "From ${folderLabel(item.parent)}"
    val size = if (item.directory) null else formatSize(item.size)
    val deleted = "Deleted ${formatTime(item.deletedAt)}"
    val leading: @Composable () -> Unit = {
        Icon(
            if (item.directory) Icons.Outlined.Folder else Icons.Outlined.Description,
            contentDescription = if (item.directory) "Folder" else "File",
        )
    }
    val trailing: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onRestore) { Text("Restore") }
            IconButton(onClick = onPurge) {
                Icon(Icons.Outlined.DeleteForever, contentDescription = "Delete ${item.name} forever")
            }
        }
    }
    if (wide) {
        ColumnsRow(item.name, listOf(folder to FOLDER_COLUMN, size.orEmpty() to SIZE_COLUMN, deleted to TIME_COLUMN), leading, trailing)
    } else {
        ListItem(
            headlineContent = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text(listOfNotNull(folder, size, deleted).joinToString(" · ")) },
            leadingContent = leading,
            trailingContent = trailing,
        )
    }
}

@Composable
private fun PurgeDialog(
    request: Purge,
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, body, confirm) =
        when (request) {
            Purge.All -> Triple("Empty trash?", "All $count items are deleted forever. This cannot be undone.", "Empty trash")
            is Purge.One -> Triple("Delete ${request.item.name} forever?", "It cannot be restored afterwards.", "Delete forever")
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep") } },
    )
}
