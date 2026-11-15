package com.anjo.filebrowser.ui.platform

interface SettingsStorage {
    fun read(key: String): String?

    fun write(
        key: String,
        value: String,
    )
}

expect fun settingsStorage(): SettingsStorage
