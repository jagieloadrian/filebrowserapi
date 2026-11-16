package com.anjo.filebrowser.ui.theme

import com.anjo.filebrowser.ui.FakeSettingsStorage
import com.anjo.filebrowser.ui.model.Appearance
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe

class AppearanceSettingsTest :
    FunSpec({
        context("should load the default theme and mode when the stored value is missing or unknown") {
            withTests(nameFn = { "stored value '$it'" }, null, "", "unknown", "BLUE") { stored ->
                // given
                val storage = FakeSettingsStorage(stored?.let { mutableMapOf(PALETTE_KEY to it, MODE_KEY to it) } ?: mutableMapOf())

                // when
                val result = AppearanceSettings(storage).state.value

                // then
                result shouldBe Appearance(Palette.BLUE, ThemeMode.SYSTEM)
            }
        }

        test("should load the stored theme and mode") {
            // given
            val storage = FakeSettingsStorage(mutableMapOf(PALETTE_KEY to "green", MODE_KEY to "dark"))

            // when
            val result = AppearanceSettings(storage).state.value

            // then
            result shouldBe Appearance(Palette.GREEN, ThemeMode.DARK)
        }

        test("should apply and store the chosen theme") {
            // given
            val storage = FakeSettingsStorage()
            val settings = AppearanceSettings(storage)

            // when
            settings.choose(Palette.ROSE)

            // then
            settings.state.value.palette shouldBe Palette.ROSE
            storage.values[PALETTE_KEY] shouldBe "rose"
        }

        test("should apply and store the chosen mode") {
            // given
            val storage = FakeSettingsStorage()
            val settings = AppearanceSettings(storage)

            // when
            settings.choose(ThemeMode.LIGHT)

            // then
            settings.state.value.mode shouldBe ThemeMode.LIGHT
            storage.values[MODE_KEY] shouldBe "light"
        }
    })
