package com.anjo.filebrowser.ui

import com.anjo.filebrowser.ui.platform.SettingsStorage

class FakeSettingsStorage(
    val values: MutableMap<String, String> = mutableMapOf(),
) : SettingsStorage {
    override fun read(key: String): String? = values[key]

    override fun write(
        key: String,
        value: String,
    ) {
        values[key] = value
    }
}
