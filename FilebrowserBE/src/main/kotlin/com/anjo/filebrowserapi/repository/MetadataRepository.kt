package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.EntryMeta
import com.anjo.filebrowserapi.model.FileHash
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.core.substring
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.springframework.stereotype.Repository

private val PATH_TABLES = listOf(EntryMetaTable, FileHashTable, FileVersionTable, ShareLinkTable)

@Repository
class MetadataRepository(
    private val db: Database,
) {
    fun findUnder(
        owner: String,
        path: String,
    ): Map<String, EntryMeta> =
        transaction(db) {
            EntryMetaTable
                .selectAll()
                .where { EntryMetaTable.under(owner, path) }
                .map { it.toEntryMeta() }
                .associateBy { it.path }
        }

    fun save(
        owner: String,
        path: String,
        description: String?,
        tags: List<String>,
    ) {
        transaction(db) {
            if (description == null && tags.isEmpty()) {
                EntryMetaTable.deleteWhere { (this.owner eq owner) and (this.path eq path) }
            } else {
                EntryMetaTable.upsert {
                    it[this.owner] = owner
                    it[this.path] = path
                    it[this.description] = description
                    it[this.tags] = tags
                }
            }
        }
    }

    fun relocate(
        owner: String,
        from: String,
        to: String,
    ) {
        transaction(db) {
            forgetRows(owner, to)
            PATH_TABLES.forEach { table ->
                table.update({ table.under(owner, from) }) { it[table.path] = table.moved(from, to) }
            }
        }
    }

    fun copy(
        owner: String,
        from: String,
        to: String,
    ) {
        transaction(db) {
            forgetRows(owner, to)
            val table = EntryMetaTable
            table.insert(
                table.select(table.owner, table.moved(from, to), table.description, table.tags).where { table.under(owner, from) },
                listOf(table.owner, table.path, table.description, table.tags),
            )
        }
    }

    fun findHashes(
        owner: String,
        path: String,
    ): Map<String, FileHash> =
        transaction(db) {
            FileHashTable
                .selectAll()
                .where { FileHashTable.under(owner, path) }
                .map { it.toFileHash() }
                .associateBy { it.path }
        }

    fun saveHashes(
        owner: String,
        hashes: List<FileHash>,
    ) {
        if (hashes.isEmpty()) return
        transaction(db) {
            FileHashTable.batchUpsert(hashes, shouldReturnGeneratedValues = false) { hash ->
                this[FileHashTable.owner] = owner
                this[FileHashTable.path] = hash.path
                this[FileHashTable.size] = hash.size
                this[FileHashTable.modifiedMs] = hash.modifiedMs
                this[FileHashTable.sha256] = hash.sha256
            }
        }
    }

    fun forget(
        owner: String,
        path: String,
    ) {
        transaction(db) { forgetRows(owner, path) }
    }

    private fun forgetRows(
        owner: String,
        path: String,
    ) {
        PATH_TABLES.forEach { table -> table.deleteWhere { table.under(owner, path) } }
    }

    private fun OwnedPathTable.moved(
        from: String,
        to: String,
    ): Expression<String> = stringLiteral(to) + path.substring(from.length + 1, Int.MAX_VALUE)

    private fun ResultRow.toEntryMeta() = EntryMeta(this[EntryMetaTable.path], this[EntryMetaTable.description], this[EntryMetaTable.tags])

    private fun ResultRow.toFileHash() =
        FileHash(this[FileHashTable.path], this[FileHashTable.size], this[FileHashTable.modifiedMs], this[FileHashTable.sha256])
}
