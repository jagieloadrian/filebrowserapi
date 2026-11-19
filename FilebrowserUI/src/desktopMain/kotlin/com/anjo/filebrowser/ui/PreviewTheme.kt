package com.anjo.filebrowser.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anjo.filebrowser.ui.auth.AuthSession
import com.anjo.filebrowser.ui.auth.LoopbackBrowser
import com.anjo.filebrowser.ui.model.Quota
import com.anjo.filebrowser.ui.platform.SettingsStorage
import com.anjo.filebrowser.ui.theme.AppearanceSettings
import com.anjo.filebrowser.ui.theme.FilebrowserTheme
import com.anjo.filebrowser.ui.theme.MODE_KEY
import com.anjo.filebrowser.ui.theme.PALETTE_KEY
import com.anjo.filebrowser.ui.theme.Palette
import com.anjo.filebrowser.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.compose.KoinApplicationPreview
import org.koin.dsl.module

internal const val PHONE_WIDTH_DP = 360
internal const val PHONE_HEIGHT_DP = 760
internal const val COLUMNS_WIDTH_DP = 840
internal const val MEDIUM_WIDTH_DP = 900
internal const val MEDIUM_HEIGHT_DP = 700
internal const val WIDE_WIDTH_DP = 1440
internal const val WIDE_HEIGHT_DP = 900
internal const val DIALOG_WIDTH_DP = 480
internal const val DIALOG_HEIGHT_DP = 480
internal const val MEDIUM_TEXT = 1.5f
internal const val LARGE_TEXT = 2f
internal const val ROW_MENU_HEIGHT_DP = 360

internal val PREVIEW_QUOTA = Quota(used = 7_730_941_133, limit = 10_737_418_240)

@Composable
internal fun PreviewTheme(
    palette: Palette = Palette.BLUE,
    mode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit,
) {
    val stored = mapOf(PALETTE_KEY to palette.name.lowercase(), MODE_KEY to mode.name.lowercase())
    val storage =
        object : SettingsStorage {
            override fun read(key: String): String? = stored[key]

            override fun write(
                key: String,
                value: String,
            ) = Unit
        }
    KoinApplicationPreview(
        application = {
            modules(
                module {
                    single { AuthSession(oidc = null, browser = LoopbackBrowser(), scope = CoroutineScope(SupervisorJob())) }
                    single { AppearanceSettings(storage) }
                },
            )
        },
    ) {
        FilebrowserTheme { Surface(Modifier.fillMaxSize(), content = content) }
    }
}
