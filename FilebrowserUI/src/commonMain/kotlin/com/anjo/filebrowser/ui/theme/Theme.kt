package com.anjo.filebrowser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val content = 720.dp
    val wide = 600.dp
    val expanded = 840.dp
}

@Composable
fun FilebrowserTheme(
    settings: AppearanceSettings = koinInject(),
    systemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appearance by settings.state.collectAsState()
    val dark =
        when (appearance.mode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    val colorScheme = remember(appearance.palette, dark) { appearance.palette.colorScheme(dark) }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
