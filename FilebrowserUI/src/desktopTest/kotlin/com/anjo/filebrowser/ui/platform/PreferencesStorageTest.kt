package com.anjo.filebrowser.ui.platform

import com.anjo.filebrowser.ui.model.Appearance
import com.anjo.filebrowser.ui.theme.AppearanceSettings
import com.anjo.filebrowser.ui.theme.Palette
import com.anjo.filebrowser.ui.theme.ThemeMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID
import java.util.prefs.Preferences

class PreferencesStorageTest :
    FunSpec({
        lateinit var node: Preferences

        beforeTest { node = Preferences.userRoot().node("filebrowser-test-${UUID.randomUUID()}") }

        afterTest {
            node.removeNode()
            Preferences.userRoot().flush()
        }

        test("should load the saved theme and mode in a new session") {
            // given
            AppearanceSettings(PreferencesStorage(node)).apply {
                choose(Palette.GREEN)
                choose(ThemeMode.DARK)
            }

            // when
            val result = AppearanceSettings(PreferencesStorage(node)).state.value

            // then
            result shouldBe Appearance(Palette.GREEN, ThemeMode.DARK)
        }

        test("should load the default theme and mode when nothing is saved") {
            // when
            val result = AppearanceSettings(PreferencesStorage(node)).state.value

            // then
            result shouldBe Appearance(Palette.BLUE, ThemeMode.SYSTEM)
        }
    })
