package com.anjo.filebrowserapi.model

import com.anjo.filebrowserapi.validation.MAX_DESCRIPTION
import com.anjo.filebrowserapi.validation.MAX_TAGS
import com.anjo.filebrowserapi.validation.ValidTags
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class Entry(
    val path: String,
    val directory: Boolean,
    val size: Long,
    val modified: Instant,
    val contentType: String?,
    val description: String? = null,
    val tags: List<String> = emptyList(),
)

data class EntryMeta(
    val path: String,
    val description: String?,
    val tags: List<String>,
)

data class EntryMetaRequest(
    @field:Size(max = MAX_DESCRIPTION)
    val description: String? = null,
    @field:Size(max = MAX_TAGS)
    @field:ValidTags
    val tags: List<String> = emptyList(),
)

data class FileHash(
    val path: String,
    val size: Long,
    val modifiedMs: Long,
    val sha256: String,
)

data class DuplicateGroup(
    val sha256: String,
    val size: Long,
    val paths: List<String>,
)

data class TrashItem(
    val id: UUID,
    val path: String,
    val directory: Boolean,
    val size: Long,
    val deletedAt: Instant,
)
