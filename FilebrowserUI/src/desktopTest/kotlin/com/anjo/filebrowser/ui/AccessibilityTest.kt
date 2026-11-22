package com.anjo.filebrowser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anjo.filebrowser.ui.auth.AuthSession
import com.anjo.filebrowser.ui.auth.FakeBrowser
import com.anjo.filebrowser.ui.auth.FakeIdentityProvider
import com.anjo.filebrowser.ui.auth.settled
import com.anjo.filebrowser.ui.common.formatTime
import com.anjo.filebrowser.ui.files.PREVIEW_LINE
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.koinConfiguration
import kotlin.time.Instant

private val COMPACT_WIDTH = 320.dp
private val WIDE_WIDTH = 1200.dp
private val MEDIUM_WIDTH = 1080.dp
private val HEIGHT = 760.dp
private val PHONE_WIDTH = 360.dp
private const val LARGE_TEXT = 2f
private val WINDOW = Size(WIDE_WIDTH.value, HEIGHT.value)
private val MIN_TARGET = 48.dp
private const val USER = "alice"
private const val ACCOUNT_MENU = "Account menu for $USER"
private const val PREVIEW_TIMEOUT_MS = 5_000L

private val TOP_BAR = listOf("New folder", "Refresh", ACCOUNT_MENU)
private val DESTINATIONS = listOf("Files", "Trash")
private val FOLDER =
    listOf("Search in this folder", "Home", "budget.xlsx", "Actions for budget.xlsx", "plan.md", "Actions for plan.md", "Upload file")

private data class FocusOrder(
    val layout: String,
    val width: Dp,
    val stops: List<String>,
)

@OptIn(ExperimentalTestApi::class)
private data class Screen(
    val name: String,
    val width: Dp,
    val fontScale: Float = 1f,
    val open: ComposeUiTest.() -> Unit = {},
)

private fun SemanticsNode.texts(): List<String> = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

private fun SemanticsNode.label(): String =
    (config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() + texts()).joinToString(" ").trim()

private fun SemanticsNode.name(): String = texts().firstOrNull() ?: label()

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.openTrash() = onNode(isSelectable() and hasText("Trash")).performClick()

@OptIn(ExperimentalTestApi::class)
class AccessibilityTest :
    FunSpec({
        lateinit var scope: CoroutineScope
        lateinit var api: FakeFilebrowserApi

        beforeTest {
            scope = CoroutineScope(SupervisorJob())
            api = FakeFilebrowserApi()
            api.entries += listOf(folder("/Work"), file("/notes.txt", size = 11), file("/report.pdf", size = 2048))
            api.contents["/notes.txt"] = "hello world".encodeToByteArray()
            api.trashItems += trashed("1", "/a.txt")
        }

        afterTest {
            stopKoin()
            scope.cancel()
        }

        suspend fun signedIn() =
            AuthSession(FakeIdentityProvider(user = USER).oidc(), FakeBrowser(), scope)
                .apply {
                    signIn()
                }.also { it.settled() }

        context("should give every control a label and a touch target of at least 48 x 48 dp") {
            withTests(
                nameFn = { it.name },
                Screen("files wide", WIDE_WIDTH),
                Screen("files compact", COMPACT_WIDTH),
                Screen("files in a folder", COMPACT_WIDTH) { onNodeWithText("Work").performClick() },
                Screen("files searching", COMPACT_WIDTH) { onNodeWithText("Search in this folder").performTextInput("o") },
                Screen("files row menu", COMPACT_WIDTH) { onNodeWithContentDescription("Actions for notes.txt").performClick() },
                Screen("account menu", COMPACT_WIDTH) { onNodeWithContentDescription(ACCOUNT_MENU).performClick() },
                Screen("new folder dialog", COMPACT_WIDTH) { onNodeWithContentDescription("New folder").performClick() },
                Screen("rename dialog", COMPACT_WIDTH) {
                    onNodeWithContentDescription("Actions for report.pdf").performClick()
                    onNodeWithText("Rename").performClick()
                },
                Screen("text preview dialog", WIDE_WIDTH) {
                    onNodeWithText("notes.txt").performClick()
                    waitUntilAtLeastOneExists(hasTestTag(PREVIEW_LINE), PREVIEW_TIMEOUT_MS)
                },
                Screen("trash wide", WIDE_WIDTH) { openTrash() },
                Screen("trash medium", MEDIUM_WIDTH) { openTrash() },
                Screen("trash compact", COMPACT_WIDTH) { openTrash() },
                Screen("trash row menu wide", WIDE_WIDTH) {
                    openTrash()
                    onNodeWithContentDescription("Actions for a.txt").performClick()
                },
                Screen("trash row menu compact", COMPACT_WIDTH) {
                    openTrash()
                    onNodeWithContentDescription("Actions for a.txt").performClick()
                },
                Screen("delete forever dialog", COMPACT_WIDTH) {
                    openTrash()
                    onNodeWithContentDescription("Actions for a.txt").performClick()
                    onNodeWithText("Delete forever").performClick()
                },
                Screen("files more options at large text", PHONE_WIDTH, LARGE_TEXT) {
                    onNodeWithContentDescription("More options").performClick()
                },
                Screen("trash more options at large text", PHONE_WIDTH, LARGE_TEXT) {
                    openTrash()
                    onNodeWithContentDescription("More options").performClick()
                },
                Screen("empty trash dialog", WIDE_WIDTH) {
                    openTrash()
                    onNodeWithText("Empty trash").performClick()
                },
            ) { screen ->
                // given
                val session = signedIn()
                runSkikoComposeUiTest(WINDOW) {
                    setContent {
                        KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, screen.fontScale)) {
                                Box(Modifier.requiredSize(screen.width, HEIGHT)) { Browser() }
                            }
                        }
                    }

                    // when
                    screen.open(this)

                    // then
                    val controls = onAllNodes(hasClickAction() or isToggleable()).fetchSemanticsNodes()
                    controls.shouldNotBeEmpty()
                    controls.forEach { node ->
                        val touch = node.touchBoundsInRoot
                        withClue("${screen.name}: ${node.config}") {
                            node.label().shouldNotBeBlank()
                            with(density) { touch.width.toDp() } shouldBeGreaterThanOrEqualTo MIN_TARGET
                            with(density) { touch.height.toDp() } shouldBeGreaterThanOrEqualTo MIN_TARGET
                        }
                    }
                }
            }
        }

        context("should move the keyboard focus through the destinations, the top app bar and the rows of the files screen") {
            withTests(
                nameFn = { it.layout },
                FocusOrder("tabs below the top app bar", COMPACT_WIDTH, TOP_BAR + DESTINATIONS + FOLDER),
                FocusOrder("rail before the top app bar", WIDE_WIDTH, DESTINATIONS + TOP_BAR + FOLDER),
            ) { order ->
                // given
                api.entries += listOf(file("/Work/budget.xlsx"), file("/Work/plan.md"))
                val session = signedIn()
                runSkikoComposeUiTest(WINDOW) {
                    lateinit var focus: FocusManager
                    setContent {
                        focus = LocalFocusManager.current
                        KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                            Box(Modifier.requiredSize(order.width, HEIGHT)) { Browser() }
                        }
                    }
                    onNodeWithText("Work").performClick()
                    runOnIdle { focus.clearFocus() }

                    // when
                    val visited =
                        order.stops.map {
                            onAllNodes(isRoot()).onFirst().performKeyInput { pressKey(Key.Tab) }
                            onNode(isFocused()).fetchSemanticsNode().name()
                        }

                    // then
                    visited shouldBe order.stops
                }
            }
        }

        context("should read a trash row as one node with its name, original location, size and time") {
            withTests(nameFn = { it.first }, "wide" to WIDE_WIDTH, "compact" to COMPACT_WIDTH) { (_, width) ->
                // given
                val session = signedIn()
                runSkikoComposeUiTest(WINDOW) {
                    setContent {
                        KoinApplication(configuration = koinConfiguration { modules(backend(api, session)) }) {
                            Box(Modifier.requiredSize(width, HEIGHT)) { Browser() }
                        }
                    }

                    // when
                    openTrash()

                    // then
                    val row = onNode(hasText("a.txt") and hasClickAction()).fetchSemanticsNode().label()
                    listOf("a.txt", "Home", "1 B", formatTime(Instant.parse("2026-11-13T10:00:00Z"))).forEach { row shouldContain it }
                }
            }
        }
    })
