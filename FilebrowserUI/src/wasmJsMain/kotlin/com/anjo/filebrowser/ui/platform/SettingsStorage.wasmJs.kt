package com.anjo.filebrowser.ui.platform

import kotlinx.browser.localStorage

private object LocalStorage : SettingsStorage {
    override fun read(key: String): String? =
        try {
            localStorage.getItem(key)
        } catch (e: Throwable) {
            println("Could not read $key from local storage: ${e.message}")
            null
        }

    override fun write(
        key: String,
        value: String,
    ) {
        try {
            localStorage.setItem(key, value)
        } catch (e: Throwable) {
            println("Could not save $key to local storage: ${e.message}")
        }
    }
}

actual fun settingsStorage(): SettingsStorage = LocalStorage
