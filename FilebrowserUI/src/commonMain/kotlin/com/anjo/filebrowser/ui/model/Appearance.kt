package com.anjo.filebrowser.ui.model

import com.anjo.filebrowser.ui.theme.Palette
import com.anjo.filebrowser.ui.theme.ThemeMode

data class Appearance(
    val palette: Palette = Palette.BLUE,
    val mode: ThemeMode = ThemeMode.SYSTEM,
)
