package com.anjo.filebrowser.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.anjo.filebrowser.ui.common.LocalTimeOffset
import com.anjo.filebrowser.ui.common.formatTime
import com.anjo.filebrowser.ui.model.TrashItem
import com.anjo.filebrowser.ui.trash.TrashRow
import io.kotest.core.spec.style.FunSpec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class PreviewThemeTest :
    FunSpec({
        test("should show preview times in UTC whatever the zone around them") {
            // given
            val item = TrashItem("1", "/a.txt", false, 1, Instant.parse("2026-11-13T10:00:00Z"))

            runComposeUiTest {
                // when
                setContent {
                    CompositionLocalProvider(LocalTimeOffset provides { 2.hours }) {
                        PreviewTheme { TrashRow(item, wide = false, restoreButton = false, onRestore = {}, onPurge = {}) }
                    }
                }

                // then
                onNodeWithText(formatTime(item.deletedAt, Duration.ZERO), substring = true).assertIsDisplayed()
            }
        }
    })
