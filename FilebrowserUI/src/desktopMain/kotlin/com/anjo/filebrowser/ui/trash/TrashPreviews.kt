package com.anjo.filebrowser.ui.trash

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.anjo.filebrowser.ui.BrowserShell
import com.anjo.filebrowser.ui.DIALOG_HEIGHT_DP
import com.anjo.filebrowser.ui.DIALOG_WIDTH_DP
import com.anjo.filebrowser.ui.Destination
import com.anjo.filebrowser.ui.LARGE_TEXT
import com.anjo.filebrowser.ui.LargeText
import com.anjo.filebrowser.ui.MEDIUM_HEIGHT_DP
import com.anjo.filebrowser.ui.MEDIUM_TEXT
import com.anjo.filebrowser.ui.PHONE_HEIGHT_DP
import com.anjo.filebrowser.ui.PHONE_WIDTH_DP
import com.anjo.filebrowser.ui.PREVIEW_QUOTA
import com.anjo.filebrowser.ui.PreviewTheme
import com.anjo.filebrowser.ui.ROW_MENU_HEIGHT_DP
import com.anjo.filebrowser.ui.TRASH_COLUMNS_WIDTH_DP
import com.anjo.filebrowser.ui.WIDE_HEIGHT_DP
import com.anjo.filebrowser.ui.WIDE_WIDTH_DP
import com.anjo.filebrowser.ui.common.Loadable
import com.anjo.filebrowser.ui.model.TrashItem
import com.anjo.filebrowser.ui.theme.ThemeMode
import ee.schimke.composeai.preview.SettledPreview
import kotlin.time.Instant

private val ARCHIVE =
    TrashItem(
        id = "3",
        path = "/Holidays/Lisbon, Porto and the Douro valley road trip 2025 - everyone's unsorted raw photos.zip",
        directory = false,
        size = 2_684_354_560,
        deletedAt = Instant.parse("2026-11-12T19:05:00Z"),
    )

private val ITEMS =
    listOf(
        TrashItem("1", "/Work/Reports/old-draft-q3.docx", false, 1_153_434, Instant.parse("2026-11-17T15:32:00Z")),
        TrashItem("2", "/Photos/Screenshots", true, 0, Instant.parse("2026-11-16T09:14:00Z")),
        ARCHIVE,
        TrashItem("4", "/shopping-list.txt", false, 742, Instant.parse("2026-11-03T18:47:00Z")),
    )

private val LOADED = TrashUiState(items = Loadable.Ready(ITEMS), quota = PREVIEW_QUOTA)

@Composable
private fun StaticTrash(
    state: TrashUiState,
    tabs: @Composable () -> Unit = {},
    initialMenuExpanded: Boolean = false,
) = TrashContent(
    state = state,
    snackbarHost = remember { SnackbarHostState() },
    tabs = tabs,
    onRefresh = {},
    onRestore = {},
    onDeleteForever = {},
    onEmptyTrash = {},
    initialMenuExpanded = initialMenuExpanded,
)

@Composable
private fun Trash(
    state: TrashUiState,
    mode: ThemeMode = ThemeMode.LIGHT,
) = PreviewTheme(mode = mode) { StaticTrash(state) }

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP)
@Composable
private fun TrashPhonePreview() = Trash(LOADED)

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP, fontScale = MEDIUM_TEXT)
@Composable
private fun TrashPhoneMediumTextPreview() = Trash(LOADED)

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP, fontScale = LARGE_TEXT)
@Composable
private fun TrashPhoneLargeTextPreview() = Trash(LOADED)

@SettledPreview
@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP, fontScale = LARGE_TEXT)
@Composable
private fun TrashMoreOptionsLargeTextPreview() = LargeText { PreviewTheme { StaticTrash(LOADED, initialMenuExpanded = true) } }

@SettledPreview
@Preview(widthDp = PHONE_WIDTH_DP, heightDp = ROW_MENU_HEIGHT_DP)
@Composable
private fun TrashRowMenuPreview() =
    PreviewTheme {
        Column { TrashRow(ITEMS.first(), wide = false, restoreButton = false, onRestore = {}, onPurge = {}, initialMenuExpanded = true) }
    }

@SettledPreview
@Preview(widthDp = WIDE_WIDTH_DP, heightDp = ROW_MENU_HEIGHT_DP)
@Composable
private fun TrashRowMenuWidePreview() =
    PreviewTheme {
        Column { TrashRow(ITEMS.first(), wide = true, restoreButton = true, onRestore = {}, onPurge = {}, initialMenuExpanded = true) }
    }

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP)
@Composable
private fun TrashPhoneDarkPreview() = Trash(LOADED, ThemeMode.DARK)

@Preview(widthDp = TRASH_COLUMNS_WIDTH_DP, heightDp = MEDIUM_HEIGHT_DP)
@Composable
private fun TrashNarrowestColumnsPreview() = Trash(LOADED)

@Preview(widthDp = WIDE_WIDTH_DP, heightDp = WIDE_HEIGHT_DP)
@Composable
private fun TrashWidePreview() = Trash(LOADED)

@Preview(widthDp = WIDE_WIDTH_DP, heightDp = WIDE_HEIGHT_DP)
@Composable
private fun AppShellTrashWidePreview() =
    PreviewTheme { BrowserShell(Destination.TRASH, onNavigate = {}) { StaticTrash(LOADED, tabs = it) } }

@SettledPreview
@Preview(widthDp = DIALOG_WIDTH_DP, heightDp = DIALOG_HEIGHT_DP)
@Composable
private fun EmptyTrashDialogPreview() = PreviewTheme { PurgeDialog(Purge.All, count = ITEMS.size, onConfirm = {}, onDismiss = {}) }

@SettledPreview
@Preview(widthDp = DIALOG_WIDTH_DP, heightDp = DIALOG_HEIGHT_DP)
@Composable
private fun DeleteForeverDialogPreview() = DeleteForeverDialog()

@SettledPreview
@Preview(widthDp = DIALOG_WIDTH_DP, heightDp = DIALOG_HEIGHT_DP)
@Composable
private fun DeleteForeverDialogDarkPreview() = DeleteForeverDialog(ThemeMode.DARK)

@SettledPreview
@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP, fontScale = LARGE_TEXT)
@Composable
private fun DeleteForeverDialogLargeTextPreview() = DeleteForeverDialog()

@Composable
private fun DeleteForeverDialog(mode: ThemeMode = ThemeMode.LIGHT) =
    PreviewTheme(mode = mode) {
        PurgeDialog(Purge.One(ARCHIVE), count = ITEMS.size, onConfirm = {}, onDismiss = {})
    }
