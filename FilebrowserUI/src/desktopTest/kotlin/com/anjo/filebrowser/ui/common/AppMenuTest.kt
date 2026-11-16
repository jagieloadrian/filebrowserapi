package com.anjo.filebrowser.ui.common

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.anjo.filebrowser.ui.FakeSettingsStorage
import com.anjo.filebrowser.ui.auth.AuthSession
import com.anjo.filebrowser.ui.auth.FakeBrowser
import com.anjo.filebrowser.ui.theme.AppearanceSettings
import com.anjo.filebrowser.ui.theme.FilebrowserTheme
import com.anjo.filebrowser.ui.theme.Palette
import com.anjo.filebrowser.ui.theme.colorScheme
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@OptIn(ExperimentalTestApi::class)
class AppMenuTest :
    FunSpec({
        lateinit var scope: CoroutineScope
        lateinit var settings: AppearanceSettings

        beforeTest {
            scope = CoroutineScope(SupervisorJob())
            settings = AppearanceSettings(FakeSettingsStorage())
        }

        afterTest { scope.cancel() }

        fun choose(choice: String): ColorScheme? {
            var applied: ColorScheme? = null
            runComposeUiTest {
                setContent {
                    FilebrowserTheme(settings, systemDark = false) {
                        val scheme = MaterialTheme.colorScheme
                        SideEffect { applied = scheme }
                        AppMenu(AuthSession(oidc = null, browser = FakeBrowser(), scope = scope), settings)
                    }
                }
                onNodeWithContentDescription("Settings").performClick()
                onNodeWithText(choice).performClick()
                waitForIdle()
                onNode(hasText(choice)).assertIsSelected()
            }
            return applied
        }

        test("should apply the primary color of the picked theme") {
            // when
            val result = choose("Green")

            // then
            result.shouldNotBeNull().primary shouldBe Palette.GREEN.colorScheme(dark = false).primary
        }

        test("should apply the dark scheme when dark is picked while the system is light") {
            // when
            val result = choose("Dark").shouldNotBeNull()

            // then
            result.primary shouldBe Palette.BLUE.colorScheme(dark = true).primary
            result.surface shouldBe Palette.BLUE.colorScheme(dark = true).surface
        }
    })
