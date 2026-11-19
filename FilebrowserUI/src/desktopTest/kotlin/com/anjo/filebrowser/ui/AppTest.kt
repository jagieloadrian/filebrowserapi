package com.anjo.filebrowser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.anjo.filebrowser.ui.api.FilebrowserApi
import com.anjo.filebrowser.ui.auth.AuthSession
import com.anjo.filebrowser.ui.auth.FakeBrowser
import com.anjo.filebrowser.ui.auth.FakeIdentityProvider
import com.anjo.filebrowser.ui.auth.settled
import com.anjo.filebrowser.ui.common.COLUMN_DIVIDER
import com.anjo.filebrowser.ui.common.ROW_DIVIDER
import com.anjo.filebrowser.ui.files.FilesViewModel
import com.anjo.filebrowser.ui.files.PREVIEW_LINE
import com.anjo.filebrowser.ui.theme.AppearanceSettings
import com.anjo.filebrowser.ui.trash.TrashViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

private val NARROWEST_WIDTH = 320.dp
private val HEIGHT = 640.dp
private val WIDE_WIDTH = 1000.dp
private val MEDIUM_WIDTH = 900.dp
private const val LARGE_TEXT = 2f
private const val LONG_NAME = "alexandra.konstantinopoulou"
private const val ACCOUNT_MENU = "Account menu for $LONG_NAME"
private const val PREVIEW_TIMEOUT_MS = 5_000L
private const val MAX_COMPOSED_LINES = 100
private const val TOOLTIP_DELAY_MS = 100L
private const val FILES_LEADING_ITEMS = 3

private data class Dividers(
    val layout: String,
    val destination: String,
    val width: Dp,
    val columnDividers: Int,
)

private data class Preview(
    val name: String,
    val bytes: ByteArray,
    val shown: SemanticsMatcher,
)

private data class TopBar(
    val destination: String,
    val actions: List<String>,
)

private fun backend(
    api: FilebrowserApi,
    session: AuthSession,
) = module {
    single { api }
    single { session }
    single { AppearanceSettings(FakeSettingsStorage()) }
    viewModel { FilesViewModel(get()) }
    viewModel { TrashViewModel(get()) }
}

private fun SemanticsNodeInteractionsProvider.destination(label: String) = onNode(isSelectable() and hasText(label))

private fun SemanticsNodeInteractionsProvider.action(label: String) = onNode(hasText(label) or hasContentDescription(label))

@OptIn(ExperimentalTestApi::class)
class AppTest :
    FunSpec({
        lateinit var scope: CoroutineScope
        lateinit var api: FakeFilebrowserApi

        beforeTest {
            scope = CoroutineScope(SupervisorJob())
            api = FakeFilebrowserApi()
            api.trashItems += trashed("1", "/a.txt")
        }

        afterTest {
            stopKoin()
            scope.cancel()
        }

        suspend fun signedIn() =
            AuthSession(FakeIdentityProvider(user = LONG_NAME).oidc(), FakeBrowser(), scope)
                .apply {
                    signIn()
                }.also { it.settled() }

        test("should switch between files and trash when the window has its narrowest width") {
            val session = signedIn()
            runComposeUiTest {
                // given
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(NARROWEST_WIDTH, HEIGHT)) { Browser() }
                    }
                }
                destination("Files").assertIsDisplayed().assertIsSelected()

                // when
                destination("Trash").assertIsDisplayed().performClick()

                // then
                destination("Trash").assertIsSelected()
                onNode(isHeading() and hasText("Trash")).assertIsDisplayed()
            }
        }

        test("should spread the file list over the full width with type, size and modified columns when the window is wide") {
            // given
            val session = signedIn()
            api.entries += file("/report.pdf", size = 2048)

            runComposeUiTest {
                // when
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(WIDE_WIDTH, HEIGHT)) { Browser() }
                    }
                }

                // then
                onNodeWithText("report.pdf").assertWidthIsAtLeast(WIDE_WIDTH)
                onNodeWithText("PDF file", useUnmergedTree = true).assertIsDisplayed()
                onNodeWithText("2.0\u00A0KB", useUnmergedTree = true).assertIsDisplayed()
                onNodeWithText("2026-11-13 10:00\u00A0UTC", useUnmergedTree = true).assertIsDisplayed()
            }
        }

        test("should leave the type column out when the columns fit but the window is narrower than 1000dp") {
            // given
            val session = signedIn()
            api.entries += file("/report.pdf", size = 2048)

            runComposeUiTest {
                // when
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(MEDIUM_WIDTH, HEIGHT)) { Browser() }
                    }
                }

                // then
                onNodeWithText("Size", useUnmergedTree = true).assertIsDisplayed()
                onNodeWithText("Type", useUnmergedTree = true).assertDoesNotExist()
                onNodeWithText("PDF file", useUnmergedTree = true).assertDoesNotExist()
            }
        }

        test("should stack the file details under the name when large text does not fit the columns") {
            // given
            val session = signedIn()
            api.entries += file("/report.pdf", size = 2048)

            runComposeUiTest {
                // when
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = LARGE_TEXT)) {
                            Box(Modifier.requiredSize(WIDE_WIDTH, HEIGHT)) { Browser() }
                        }
                    }
                }

                // then
                onNodeWithText("PDF file", useUnmergedTree = true).assertDoesNotExist()
                onNodeWithText("2.0\u00A0KB · Modified 2026-11-13 10:00\u00A0UTC", useUnmergedTree = true).assertIsDisplayed()
            }
        }

        test("should show the whole name in a tooltip when the mouse rests on a name cut short by its column") {
            // given
            val session = signedIn()
            val name = "quarterly-report-".repeat(10) + "final.pdf"
            api.entries += file("/$name")
            runComposeUiTest {
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(WIDE_WIDTH, HEIGHT)) { Browser() }
                    }
                }
                mainClock.autoAdvance = false

                // when
                onNodeWithText(name, useUnmergedTree = true).performMouseInput { moveTo(center) }
                mainClock.advanceTimeBy(TOOLTIP_DELAY_MS)

                // then
                onAllNodesWithText(name, useUnmergedTree = true).fetchSemanticsNodes() shouldHaveSize 2
            }
        }

        context("should separate the rows with one divider fewer than rows and the columns only when the window is wide") {
            withTests(
                nameFn = { it.layout },
                Dividers("files wide", "Files", WIDE_WIDTH, columnDividers = 12),
                Dividers("files compact", "Files", NARROWEST_WIDTH, columnDividers = 0),
                Dividers("trash wide", "Trash", WIDE_WIDTH, columnDividers = 12),
                Dividers("trash compact", "Trash", NARROWEST_WIDTH, columnDividers = 0),
            ) { case ->
                // given
                val session = signedIn()
                api.entries += listOf(folder("/Documents"), file("/a.txt"), file("/b.txt"))
                api.trashItems += listOf(trashed("2", "/b.txt"), trashed("3", "/c.txt"))
                runComposeUiTest {
                    setContent {
                        KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                            Box(Modifier.requiredSize(case.width, HEIGHT)) { Browser() }
                        }
                    }

                    // when
                    destination(case.destination).performClick()

                    // then
                    onAllNodesWithTag(ROW_DIVIDER, useUnmergedTree = true).fetchSemanticsNodes() shouldHaveSize 2
                    onAllNodesWithTag(COLUMN_DIVIDER, useUnmergedTree = true).fetchSemanticsNodes() shouldHaveSize case.columnDividers
                }
            }
        }

        test("should draw the column dividers over the full row height when the window is wide") {
            // given
            val session = signedIn()
            api.entries += file("/report.pdf", size = 2048)

            runComposeUiTest {
                // when
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(WIDE_WIDTH, HEIGHT)) { Browser() }
                    }
                }

                // then
                val rowHeight = onNodeWithText("report.pdf").getUnclippedBoundsInRoot().height
                val dividers = onAllNodesWithTag(COLUMN_DIVIDER, useUnmergedTree = true)
                dividers.fetchSemanticsNodes() shouldHaveSize 6
                (3 until 6).forEach { dividers[it].assertHeightIsEqualTo(rowHeight) }
            }
        }

        context("should tell screen readers what tapping a row does") {
            withTests(
                nameFn = { it.first },
                "Documents" to "Open folder",
                "notes.txt" to "Preview file",
                "archive.zip" to "Show actions",
            ) { (name, expected) ->
                // given
                val session = signedIn()
                api.entries += listOf(folder("/Documents"), file("/notes.txt"), file("/archive.zip"))

                runComposeUiTest {
                    // when
                    setContent {
                        KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                            Box(Modifier.requiredSize(NARROWEST_WIDTH, HEIGHT)) { Browser() }
                        }
                    }

                    // then
                    onNodeWithText(name).fetchSemanticsNode().config[SemanticsActions.OnClick].label shouldBe expected
                }
            }
        }

        context("should name the trashed item in its row and in its actions for screen readers") {
            withTests(nameFn = { it.first }, "wide" to WIDE_WIDTH, "compact" to NARROWEST_WIDTH) { (_, width) ->
                // given
                val session = signedIn()
                runComposeUiTest {
                    setContent {
                        KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                            Box(Modifier.requiredSize(width, HEIGHT)) { Browser() }
                        }
                    }
                    destination("Trash").performClick()

                    // when
                    onNodeWithContentDescription("Actions for a.txt").performClick()

                    // then
                    onNodeWithText("a.txt").assert(hasText("2026-11-13 10:00\u00A0UTC", substring = true))
                    onNodeWithContentDescription("Restore a.txt").assertIsDisplayed()
                    onNodeWithContentDescription("Delete a.txt forever").assertIsDisplayed()
                }
            }
        }

        context("should tell screen readers a row is a folder once") {
            withTests(nameFn = { it.first }, "wide" to WIDE_WIDTH, "compact" to NARROWEST_WIDTH) { (_, width) ->
                // given
                val session = signedIn()
                api.entries += folder("/Documents")

                runComposeUiTest {
                    // when
                    setContent {
                        KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                            Box(Modifier.requiredSize(width, HEIGHT)) { Browser() }
                        }
                    }

                    // then
                    onNodeWithText("Documents")
                        .assert(hasText("Folder", substring = true))
                        .assert(!hasContentDescription("Folder"))
                }
            }
        }

        context("should preview a file when its row is clicked") {
            withTests(
                nameFn = { it.name },
                Preview("notes.txt", "hello world".encodeToByteArray(), hasText("hello world")),
                Preview(
                    "photo.png",
                    png(),
                    hasContentDescription("photo.png") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image),
                ),
            ) { case ->
                // given
                val session = signedIn()
                api.entries += file("/${case.name}", size = case.bytes.size.toLong())
                api.contents["/${case.name}"] = case.bytes
                runComposeUiTest {
                    setContent {
                        KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                            Box(Modifier.requiredSize(WIDE_WIDTH, HEIGHT)) { Browser() }
                        }
                    }

                    // when
                    onNodeWithText(case.name).performClick()

                    // then
                    onNode(isHeading() and hasText(case.name)).assertIsDisplayed()
                    waitUntilExactlyOneExists(case.shown)
                    onNode(case.shown).assertIsDisplayed()
                }
            }
        }

        context("should compose only the visible lines of a 1 MiB text preview") {
            withTests(nameFn = { if (it) "wrapped" else "not wrapped" }, true, false) { wrapped ->
                // given
                val session = signedIn()
                val text = ("x".repeat(63) + "\n").repeat(16_384).encodeToByteArray()
                api.entries += file("/big.log", size = text.size.toLong())
                api.contents["/big.log"] = text
                runComposeUiTest {
                    setContent {
                        KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                            Box(Modifier.requiredSize(WIDE_WIDTH, HEIGHT)) { Browser() }
                        }
                    }

                    // when
                    onNodeWithText("big.log").performClick()
                    waitUntilAtLeastOneExists(hasTestTag(PREVIEW_LINE), PREVIEW_TIMEOUT_MS)
                    if (!wrapped) onNodeWithContentDescription("Wrap lines").performClick()

                    // then
                    onAllNodesWithTag(PREVIEW_LINE, useUnmergedTree = true).fetchSemanticsNodes().size shouldBeInRange 1..MAX_COMPOSED_LINES
                }
            }
        }

        context("should offer Preview as the first row menu item only for files it can show") {
            withTests(
                nameFn = { it.first },
                "notes.txt" to true,
                "archive.zip" to false,
            ) { (name, offered) ->
                // given
                val session = signedIn()
                api.entries += file("/$name")
                runComposeUiTest {
                    setContent {
                        KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                            Box(Modifier.requiredSize(NARROWEST_WIDTH, HEIGHT)) { Browser() }
                        }
                    }

                    // when
                    onNodeWithContentDescription("Actions for $name").performClick()

                    // then
                    val download = onNodeWithText("Download").assertIsDisplayed().getUnclippedBoundsInRoot()
                    if (offered) {
                        onNodeWithText("Preview").getUnclippedBoundsInRoot().bottom shouldBeLessThanOrEqualTo download.top
                    } else {
                        onNodeWithText("Preview").assertDoesNotExist()
                    }
                }
            }
        }

        test("should open the preview from the row menu") {
            // given
            val session = signedIn()
            api.entries += file("/notes.txt", size = 11)
            api.contents["/notes.txt"] = "hello world".encodeToByteArray()
            runComposeUiTest {
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(NARROWEST_WIDTH, HEIGHT)) { Browser() }
                    }
                }
                onNodeWithContentDescription("Actions for notes.txt").performClick()

                // when
                onNodeWithText("Preview").performClick()

                // then
                onNode(isHeading() and hasText("notes.txt")).assertIsDisplayed()
                waitUntilExactlyOneExists(hasText("hello world"), PREVIEW_TIMEOUT_MS)
            }
        }

        test("should highlight code in a Kotlin preview until highlighting is turned off") {
            // given
            val session = signedIn()
            val code = "fun main() = println(\"hi\") // greet".encodeToByteArray()
            api.entries += file("/Main.kt", size = code.size.toLong())
            api.contents["/Main.kt"] = code
            runComposeUiTest {
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(WIDE_WIDTH, HEIGHT)) { Browser() }
                    }
                }
                onNodeWithText("Main.kt").performClick()
                waitUntilExactlyOneExists(hasTestTag(PREVIEW_LINE), PREVIEW_TIMEOUT_MS)

                fun spans() =
                    onNode(hasTestTag(PREVIEW_LINE), useUnmergedTree = true)
                        .fetchSemanticsNode()
                        .config[SemanticsProperties.Text]
                        .flatMap { it.spanStyles }
                spans() shouldHaveSize 3

                // when
                onNodeWithContentDescription("Highlight code").assertIsOn().performClick()

                // then
                spans() shouldBe emptyList()
            }
        }

        test("should close the preview and cancel its download when Escape is pressed") {
            // given
            val session = signedIn()
            api.entries += file("/notes.txt")
            api.downloadGate = CompletableDeferred()
            runComposeUiTest {
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(WIDE_WIDTH, HEIGHT)) { Browser() }
                    }
                }
                onNodeWithText("notes.txt").performClick()
                onNode(isHeading() and hasText("notes.txt")).assertIsDisplayed()

                // when
                onNode(isFocused() and hasAnyDescendant(isHeading() and hasText("notes.txt"))).performKeyInput { pressKey(Key.Escape) }

                // then
                onNode(isHeading() and hasText("notes.txt")).assertDoesNotExist()
                waitUntil(timeoutMillis = PREVIEW_TIMEOUT_MS) { api.cancelledDownloads.isNotEmpty() }
                api.cancelledDownloads shouldBe listOf("/notes.txt")
            }
        }

        test("should offer a download instead of a preview when the image is too large") {
            // given
            val session = signedIn()
            api.entries += file("/huge.png", size = 21L * 1024 * 1024)
            runComposeUiTest {
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(NARROWEST_WIDTH, HEIGHT)) { Browser() }
                    }
                }

                // when
                onNodeWithText("huge.png").performClick()

                // then
                onNodeWithText("Too large to preview").assertIsDisplayed()
                onNodeWithText("Download file").assertIsDisplayed()
                api.calls.filter { it.startsWith("download") } shouldBe emptyList()
            }
        }

        test("should keep the breadcrumb row height when the path grows from Home to a folder") {
            // given
            val session = signedIn()
            api.entries += folder("/Documents")
            runComposeUiTest {
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(NARROWEST_WIDTH, HEIGHT)) { Browser() }
                    }
                }
                val home = onNode(isHeading() and hasText("Home")).getUnclippedBoundsInRoot()

                // when
                onNodeWithText("Documents").performClick()

                // then
                val documents = onNode(isHeading() and hasText("Documents")).getUnclippedBoundsInRoot()
                documents.top shouldBe home.top
            }
        }

        test("should scroll the breadcrumbs to the current folder when the path grows wider than the window") {
            // given
            val session = signedIn()
            api.entries += listOf(folder("/Quarterly reports 2026"), folder("/Quarterly reports 2026/Northern region final"))
            runComposeUiTest {
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(NARROWEST_WIDTH, HEIGHT)) { Browser() }
                    }
                }
                onNodeWithText("Quarterly reports 2026").performClick()

                // when
                onNodeWithText("Northern region final").performClick()

                // then
                onNode(isHeading() and hasText("Northern region final")).getUnclippedBoundsInRoot().right shouldBeLessThanOrEqualTo
                    NARROWEST_WIDTH
            }
        }

        test("should mark the empty folder title as a heading") {
            // given
            val session = signedIn()

            runComposeUiTest {
                // when
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(NARROWEST_WIDTH, HEIGHT)) { Browser() }
                    }
                }

                // then
                onNode(isHeading() and hasText("This folder is empty")).assertIsDisplayed()
            }
        }

        test("should keep the last file row clear of the upload button when the list is scrolled to the end") {
            // given
            val session = signedIn()
            api.entries += (10..29).map { file("/file-$it.txt") }
            runComposeUiTest {
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(NARROWEST_WIDTH, HEIGHT)) { Browser() }
                    }
                }

                // when
                onNode(hasScrollToIndexAction()).performScrollToIndex(FILES_LEADING_ITEMS + 19)

                // then
                val lastActions = onNodeWithContentDescription("Actions for file-29.txt").getUnclippedBoundsInRoot()
                val upload =
                    onNode(
                        hasClickAction() and hasAnyDescendant(hasText("Upload file")),
                        useUnmergedTree = true,
                    ).getUnclippedBoundsInRoot()
                lastActions.bottom shouldBeLessThanOrEqualTo upload.top
            }
        }

        test("should scroll the quota, the search field and the breadcrumbs away with the file list") {
            // given
            val session = signedIn()
            api.entries += (10..29).map { file("/file-$it.txt") }
            runComposeUiTest {
                setContent {
                    KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                        Box(Modifier.requiredSize(NARROWEST_WIDTH, HEIGHT)) { Browser() }
                    }
                }
                onNodeWithText("Search in this folder").assertIsDisplayed()

                // when
                onNode(hasScrollToIndexAction()).performScrollToIndex(FILES_LEADING_ITEMS + 19)

                // then
                onNodeWithText("Search in this folder").assertDoesNotExist()
                onNode(isHeading() and hasText("Home")).assertDoesNotExist()
            }
        }

        context("should keep the title and every top bar action reachable when the window has its narrowest width") {
            withTests(
                nameFn = { it.destination },
                TopBar("Files", listOf("New folder", "Refresh", ACCOUNT_MENU)),
                TopBar("Trash", listOf("Empty trash", "Refresh", ACCOUNT_MENU)),
            ) { topBar ->
                val session = signedIn()
                runComposeUiTest {
                    // given
                    setContent {
                        KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                            Box(Modifier.requiredSize(NARROWEST_WIDTH, HEIGHT)) { Browser() }
                        }
                    }

                    // when
                    destination(topBar.destination).performClick()

                    // then
                    onNode(isHeading() and hasText(topBar.destination)).assertIsDisplayed()
                    topBar.actions.forEach { action(it).assertIsDisplayed() }
                    onNodeWithContentDescription(ACCOUNT_MENU).performClick()
                    onNodeWithText("Sign out").assertIsDisplayed()
                }
            }
        }
    })
