package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.FileVersion
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class VersionRepository(
    private val db: Database,
) {
    fun insert(
        owner: String,
        version: FileVersion,
    ) {
        transaction(db) {
            FileVersionTable.insert {
                it[id] = version.id
                it[this.owner] = owner
                it[path] = version.path
                it[size] = version.size
                it[createdAt] = version.createdAt.utc()
            }
        }
    }

    fun list(
        owner: String,
        path: String,
    ): List<FileVersion> =
        transaction(db) {
            FileVersionTable
                .selectAll()
                .where { (FileVersionTable.owner eq owner) and (FileVersionTable.path eq path) }
                .orderBy(FileVersionTable.createdAt to SortOrder.DESC, FileVersionTable.id to SortOrder.ASC)
                .map { it.toVersion() }
        }

    fun find(
        owner: String,
        id: UUID,
    ): FileVersion? =
        transaction(db) {
            FileVersionTable
                .selectAll()
                .where { (FileVersionTable.owner eq owner) and (FileVersionTable.id eq id) }
                .singleOrNull()
                ?.toVersion()
        }

    fun idsUnder(
        owner: String,
        path: String,
    ): List<UUID> =
        transaction(db) {
            FileVersionTable.select(FileVersionTable.id).where { FileVersionTable.under(owner, path) }.map { it[FileVersionTable.id] }
        }

    fun delete(
        owner: String,
        id: UUID,
    ) {
        transaction(db) { FileVersionTable.deleteWhere { (this.owner eq owner) and (this.id eq id) } }
    }

    private fun ResultRow.toVersion() =
        FileVersion(
            this[FileVersionTable.id],
            this[FileVersionTable.path],
            this[FileVersionTable.size],
            this[FileVersionTable.createdAt].toInstant(),
        )
}
