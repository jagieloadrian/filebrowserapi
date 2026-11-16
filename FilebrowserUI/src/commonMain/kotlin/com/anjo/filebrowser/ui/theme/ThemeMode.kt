package com.anjo.filebrowser.ui.theme

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun of(id: String?): ThemeMode = entries.firstOrNull { it.name.lowercase() == id } ?: SYSTEM
    }
}
