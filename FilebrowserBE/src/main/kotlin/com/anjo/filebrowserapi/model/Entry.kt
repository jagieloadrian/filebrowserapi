package com.anjo.filebrowserapi.model

import java.time.Instant

data class Entry(
    val path: String,
    val directory: Boolean,
    val size: Long,
    val modified: Instant,
    val contentType: String?,
)
