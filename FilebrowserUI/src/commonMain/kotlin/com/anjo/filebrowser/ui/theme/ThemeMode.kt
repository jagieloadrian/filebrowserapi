package com.anjo.filebrowser.ui.theme

enum class ThemeMode(
    val id: String,
    val label: String,
) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    ;

    companion object {
        fun of(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}
