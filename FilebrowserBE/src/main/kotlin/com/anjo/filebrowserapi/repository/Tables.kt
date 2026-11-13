package com.anjo.filebrowserapi.repository

import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

abstract class OwnedPathTable(
    name: String,
) : Table(name) {
    val owner = text("owner")
    val path = text("path")

    fun under(
        owner: String,
        path: String,
    ): Op<Boolean> = (this.owner eq owner) and ((this.path eq path) or (this.path like LikePattern.ofLiteral(path.trimEnd('/')) + "/%"))
}

object EntryMetaTable : OwnedPathTable("entry_meta") {
    val description = text("description").nullable()
    val tags = array<String>("tags")
    override val primaryKey = PrimaryKey(owner, path)
}

object FileHashTable : OwnedPathTable("file_hash") {
    val size = long("size")
    val modifiedMs = long("modified_ms")
    val sha256 = text("sha256")
    override val primaryKey = PrimaryKey(owner, path)
}

object FileVersionTable : OwnedPathTable("file_version") {
    val id = javaUUID("id")
    val size = long("size")
    val createdAt = timestampWithTimeZone("created_at")
    override val primaryKey = PrimaryKey(id)
}

object ShareLinkTable : OwnedPathTable("share_link") {
    val id = javaUUID("id")
    val tokenHash = text("token_hash")
    val createdAt = timestampWithTimeZone("created_at")
    val expiresAt = timestampWithTimeZone("expires_at")
    override val primaryKey = PrimaryKey(id)
}

object TrashItemTable : Table("trash_item") {
    val id = javaUUID("id")
    val owner = text("owner")
    val originalPath = text("original_path")
    val directory = bool("directory")
    val size = long("size")
    val deletedAt = timestampWithTimeZone("deleted_at")
    override val primaryKey = PrimaryKey(id)
}

internal fun Instant.utc(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)
