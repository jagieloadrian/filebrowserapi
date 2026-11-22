package com.anjo.filebrowser.ui.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.anjo.filebrowser.ui.COLUMNS_WIDTH_DP
import com.anjo.filebrowser.ui.DIALOG_HEIGHT_DP
import com.anjo.filebrowser.ui.DIALOG_WIDTH_DP
import com.anjo.filebrowser.ui.Destination
import com.anjo.filebrowser.ui.DestinationTabs
import com.anjo.filebrowser.ui.LARGE_TEXT
import com.anjo.filebrowser.ui.MEDIUM_HEIGHT_DP
import com.anjo.filebrowser.ui.MEDIUM_TEXT
import com.anjo.filebrowser.ui.MEDIUM_WIDTH_DP
import com.anjo.filebrowser.ui.PHONE_HEIGHT_DP
import com.anjo.filebrowser.ui.PHONE_WIDTH_DP
import com.anjo.filebrowser.ui.PREVIEW_QUOTA
import com.anjo.filebrowser.ui.PreviewTheme
import com.anjo.filebrowser.ui.ROW_MENU_HEIGHT_DP
import com.anjo.filebrowser.ui.WIDE_HEIGHT_DP
import com.anjo.filebrowser.ui.WIDE_WIDTH_DP
import com.anjo.filebrowser.ui.api.ApiStatus
import com.anjo.filebrowser.ui.common.Loadable
import com.anjo.filebrowser.ui.common.NameDialog
import com.anjo.filebrowser.ui.common.Tooltip
import com.anjo.filebrowser.ui.model.Entry
import com.anjo.filebrowser.ui.model.FilePreview
import com.anjo.filebrowser.ui.model.PreviewContent
import com.anjo.filebrowser.ui.theme.Palette
import com.anjo.filebrowser.ui.theme.ThemeMode
import ee.schimke.composeai.preview.SettledPreview
import kotlin.time.Instant

private const val FOLDER = "/Work/Reports"
private const val IMAGE_WIDTH = 640
private const val IMAGE_HEIGHT = 400
private const val TOOLTIP_WIDTH_DP = 240
private const val TOOLTIP_HEIGHT_DP = 160
private const val FIRST_ROW_INDEX = 3

private fun entry(
    name: String,
    size: Long,
    modified: String,
    directory: Boolean = false,
) = Entry("$FOLDER/$name", directory, size, Instant.parse(modified))

private val CODE_SAMPLE =
    """
    package com.anjo.reports

    data class Quarter(
        val name: String,
        val revenue: Long,
    )

    fun summarize(quarters: List<Quarter>): String {
        val total = quarters.sumOf { it.revenue }
        if (total == 0L) return "No revenue yet"
        val best = quarters.maxBy { it.revenue }
        return "Best quarter: " + best.name + ", total " + total / 100 + " EUR"
    }
    """.trimIndent()

private val SPREADSHEET = entry("budget-forecast-2027.xlsx", 286_720, "2026-11-16T10:12:00Z")
private val CODE_FILE = entry("QuarterSummary.kt", 2_431, "2026-11-15T17:03:00Z")
private val PHOTO = entry("team-offsite-lisbon.jpg", 3_407_872, "2026-11-09T11:26:00Z")

private val ENTRIES =
    listOf(
        entry("Archive", 0, "2026-10-02T16:20:00Z", directory = true),
        entry("Drafts", 0, "2026-11-17T08:05:00Z", directory = true),
        entry("Quarterly reviews", 0, "2026-11-14T13:47:00Z", directory = true),
        entry(
            "Annual report 2026 - final version approved by finance, legal and the supervisory board (signed copy).pdf",
            4_718_592,
            "2026-11-18T07:58:00Z",
        ),
        entry("backup-2026-10.tar.gz", 1_288_490_189, "2026-11-01T02:00:00Z"),
        SPREADSHEET,
        entry("customer-export-2026-11.csv", 18_874_368, "2026-11-12T09:41:00Z"),
        entry("meeting-notes.md", 3_912, "2026-11-17T14:30:00Z"),
        CODE_FILE,
        PHOTO,
    )

private val LOADED = FilesUiState(path = FOLDER, entries = Loadable.Ready(ENTRIES), quota = PREVIEW_QUOTA)

private fun photo(): ImageBitmap {
    val bitmap = ImageBitmap(IMAGE_WIDTH, IMAGE_HEIGHT)
    val width = IMAGE_WIDTH.toFloat()
    val height = IMAGE_HEIGHT.toFloat()
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(width, height)) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF6FA8DC), Color(0xFFF9CB9C))))
        drawCircle(Color(0xFFFFE599), radius = height / 10, center = Offset(width * 0.75f, height * 0.25f))
        drawRect(Color(0xFF3D85C6), topLeft = Offset(0f, height * 0.6f), size = Size(width, height * 0.25f))
        drawRect(Color(0xFFF6E3B4), topLeft = Offset(0f, height * 0.85f), size = Size(width, height * 0.15f))
    }
    return bitmap
}

@Composable
private fun StaticFiles(
    state: FilesUiState,
    tabs: @Composable () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) = FilesContent(
    state = state,
    snackbarHost = remember { SnackbarHostState() },
    tabs = tabs,
    onRefresh = {},
    onUpload = {},
    onSearch = {},
    onOpen = {},
    onPreview = {},
    onClosePreview = {},
    onDownload = {},
    onCreateFolder = {},
    onRename = { _, _ -> },
    onDelete = {},
    listState = listState,
)

@Composable
private fun Files(
    state: FilesUiState,
    palette: Palette = Palette.BLUE,
    mode: ThemeMode = ThemeMode.LIGHT,
) = PreviewTheme(palette, mode) { StaticFiles(state) }

@Composable
private fun AppShell() = PreviewTheme { StaticFiles(LOADED, tabs = { DestinationTabs(Destination.FILES, onNavigate = {}) }) }

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP)
@Composable
private fun AppShellPhonePreview() = AppShell()

@Preview(widthDp = MEDIUM_WIDTH_DP, heightDp = MEDIUM_HEIGHT_DP)
@Composable
private fun AppShellMediumPreview() = AppShell()

@Preview(widthDp = WIDE_WIDTH_DP, heightDp = WIDE_HEIGHT_DP)
@Composable
private fun AppShellWidePreview() = AppShell()

@Preview(widthDp = WIDE_WIDTH_DP, heightDp = WIDE_HEIGHT_DP, fontScale = LARGE_TEXT)
@Composable
private fun AppShellWideLargeTextPreview() = AppShell()

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP)
@Composable
private fun FilesPhonePreview() = Files(LOADED)

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP, fontScale = MEDIUM_TEXT)
@Composable
private fun FilesPhoneMediumTextPreview() = Files(LOADED)

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP, fontScale = LARGE_TEXT)
@Composable
private fun FilesPhoneLargeTextPreview() = Files(LOADED)

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP, fontScale = LARGE_TEXT)
@Composable
private fun FilesPhoneLargeTextScrolledPreview() =
    PreviewTheme { StaticFiles(LOADED, listState = rememberLazyListState(initialFirstVisibleItemIndex = FIRST_ROW_INDEX)) }

@SettledPreview
@Preview(widthDp = PHONE_WIDTH_DP, heightDp = ROW_MENU_HEIGHT_DP)
@Composable
private fun FilesRowMenuPreview() =
    PreviewTheme {
        Column {
            EntryRow(
                entry = CODE_FILE,
                searching = false,
                wide = false,
                typed = false,
                onOpen = {},
                onPreview = {},
                onDownload = {},
                onRename = {},
                onDelete = {},
                initialMenuExpanded = true,
            )
        }
    }

@Preview(widthDp = TOOLTIP_WIDTH_DP, heightDp = TOOLTIP_HEIGHT_DP)
@Composable
private fun TooltipPreview() =
    PreviewTheme {
        Box(contentAlignment = Alignment.Center) {
            Tooltip("New folder", initiallyVisible = true) {
                IconButton(onClick = {}) { Icon(Icons.Outlined.CreateNewFolder, contentDescription = "New folder") }
            }
        }
    }

@Preview(widthDp = COLUMNS_WIDTH_DP, heightDp = MEDIUM_HEIGHT_DP)
@Composable
private fun FilesNarrowestColumnsPreview() = Files(LOADED)

@Preview(widthDp = WIDE_WIDTH_DP, heightDp = WIDE_HEIGHT_DP)
@Composable
private fun FilesWidePreview() = Files(LOADED)

@Preview(widthDp = WIDE_WIDTH_DP, heightDp = WIDE_HEIGHT_DP)
@Composable
private fun FilesWideDarkPreview() = Files(LOADED, mode = ThemeMode.DARK)

@Preview(widthDp = WIDE_WIDTH_DP, heightDp = WIDE_HEIGHT_DP)
@Composable
private fun FilesWideGreenPreview() = Files(LOADED, palette = Palette.GREEN)

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP)
@Composable
private fun FilesEmptyFolderPreview() = Files(LOADED.copy(path = "$FOLDER/Drafts", entries = Loadable.Ready(emptyList())))

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP)
@Composable
private fun FilesErrorPreview() = Files(FilesUiState(entries = Loadable.Failed(ApiStatus.OFFLINE.fallback)))

@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP)
@Composable
private fun FilesLoadingPreview() = Files(FilesUiState())

@SettledPreview
@Preview(widthDp = MEDIUM_WIDTH_DP, heightDp = MEDIUM_HEIGHT_DP)
@Composable
private fun TextPreviewDialogPreview() =
    Files(
        LOADED.copy(
            preview =
                FilePreview(
                    CODE_FILE,
                    Loadable.Ready(PreviewContent.Text(previewLines(CODE_SAMPLE), shownBytes = null)),
                ),
        ),
    )

@SettledPreview
@Preview(widthDp = MEDIUM_WIDTH_DP, heightDp = MEDIUM_HEIGHT_DP)
@Composable
private fun ImagePreviewDialogPreview() = Files(LOADED.copy(preview = FilePreview(PHOTO, Loadable.Ready(PreviewContent.Image(photo())))))

@SettledPreview
@Preview(widthDp = DIALOG_WIDTH_DP, heightDp = DIALOG_HEIGHT_DP)
@Composable
private fun NewFolderDialogPreview() = NewFolderDialog()

@SettledPreview
@Preview(widthDp = PHONE_WIDTH_DP, heightDp = PHONE_HEIGHT_DP, fontScale = LARGE_TEXT)
@Composable
private fun NewFolderDialogLargeTextPreview() = NewFolderDialog()

@Composable
private fun NewFolderDialog() =
    PreviewTheme { NameDialog(title = "New folder", confirmLabel = "Create folder", initial = "", onConfirm = {}, onDismiss = {}) }

@SettledPreview
@Preview(widthDp = DIALOG_WIDTH_DP, heightDp = DIALOG_HEIGHT_DP)
@Composable
private fun RenameDialogPreview() =
    PreviewTheme { NameDialog(title = "Rename", confirmLabel = "Rename", initial = SPREADSHEET.name, onConfirm = {}, onDismiss = {}) }
