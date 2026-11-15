package com.anjo.filebrowser.ui.theme

enum class Palette(
    val id: String,
    val label: String,
    val seed: Int,
) {
    BLUE("blue", "Blue", 0xFF1E6FD9.toInt()),
    GREEN("green", "Green", 0xFF2E7D32.toInt()),
    PURPLE("purple", "Purple", 0xFF6750A4.toInt()),
    ORANGE("orange", "Orange", 0xFFE8710A.toInt()),
    ROSE("rose", "Rose", 0xFFD81B60.toInt()),
    ;

    companion object {
        fun of(id: String?): Palette = entries.firstOrNull { it.id == id } ?: BLUE
    }
}
