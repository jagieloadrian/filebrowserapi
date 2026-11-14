package com.anjo.filebrowser.ui.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class TrashItem(
    val id: String,
    val path: String,
    val directory: Boolean,
    val size: Long,
    val deletedAt: Instant,
) {
    val name: String get() = path.substringAfterLast('/')
    val parent: String get() = path.substringBeforeLast('/')
}
