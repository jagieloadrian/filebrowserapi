package com.anjo.filebrowser.ui.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Entry(
    val path: String,
    val directory: Boolean,
    val size: Long,
    val modified: Instant,
    val contentType: String? = null,
) {
    val name: String get() = path.substringAfterLast('/')
    val parent: String get() = path.substringBeforeLast('/')
}
