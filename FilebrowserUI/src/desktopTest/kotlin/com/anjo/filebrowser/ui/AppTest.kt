package com.anjo.filebrowser.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.anjo.filebrowser.ui.api.FilebrowserApi
import com.anjo.filebrowser.ui.auth.AuthSession
import com.anjo.filebrowser.ui.auth.FakeBrowser
import com.anjo.filebrowser.ui.auth.FakeIdentityProvider
import com.anjo.filebrowser.ui.auth.settled
import com.anjo.filebrowser.ui.files.FilesViewModel
import com.anjo.filebrowser.ui.theme.AppearanceSettings
import com.anjo.filebrowser.ui.trash.TrashViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
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
private val NAVIGATION_RAIL = 80.dp
private const val LONG_NAME = "alexandra.konstantinopoulou"
private const val ACCOUNT_MENU = "Account menu for $LONG_NAME"

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
                onNodeWithText("report.pdf").assertWidthIsAtLeast(WIDE_WIDTH - NAVIGATION_RAIL)
                onNodeWithText("PDF file", useUnmergedTree = true).assertIsDisplayed()
                onNodeWithText("2.0 KB", useUnmergedTree = true).assertIsDisplayed()
                onNodeWithText("Modified 2026-11-13 10:00 UTC", useUnmergedTree = true).assertIsDisplayed()
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
                onNode(hasScrollToIndexAction()).performScrollToIndex(19)

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
