package com.anjo.filebrowser.ui.theme

enum class Palette(
    val seed: Int,
) {
    BLUE(0xFF1E6FD9.toInt()),
    GREEN(0xFF2E7D32.toInt()),
    PURPLE(0xFF6750A4.toInt()),
    ORANGE(0xFFE8710A.toInt()),
    ROSE(0xFFD81B60.toInt()),
    ;

    companion object {
        fun of(id: String?): Palette = entries.firstOrNull { it.name.lowercase() == id } ?: BLUE
    }
}
