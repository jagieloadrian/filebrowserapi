package com.anjo.filebrowser.ui.platform

import java.util.prefs.Preferences

private const val NODE = "com/anjo/filebrowser"

class PreferencesStorage(
    private val node: Preferences,
) : SettingsStorage {
    override fun read(key: String): String? = node.get(key, null)

    override fun write(
        key: String,
        value: String,
    ) = node.put(key, value)
}

actual fun settingsStorage(): SettingsStorage = PreferencesStorage(Preferences.userRoot().node(NODE))
