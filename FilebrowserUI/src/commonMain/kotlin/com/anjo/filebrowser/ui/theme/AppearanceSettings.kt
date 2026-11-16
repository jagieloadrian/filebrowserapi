package com.anjo.filebrowser.ui.theme

import com.anjo.filebrowser.ui.model.Appearance
import com.anjo.filebrowser.ui.platform.SettingsStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

const val PALETTE_KEY = "filebrowser.theme"
const val MODE_KEY = "filebrowser.mode"

class AppearanceSettings(
    private val storage: SettingsStorage,
) {
    private val mutableState = MutableStateFlow(Appearance(Palette.of(storage.read(PALETTE_KEY)), ThemeMode.of(storage.read(MODE_KEY))))
    val state: StateFlow<Appearance> = mutableState.asStateFlow()

    fun choose(palette: Palette) {
        mutableState.update { it.copy(palette = palette) }
        storage.write(PALETTE_KEY, palette.name.lowercase())
    }

    fun choose(mode: ThemeMode) {
        mutableState.update { it.copy(mode = mode) }
        storage.write(MODE_KEY, mode.name.lowercase())
    }
}
