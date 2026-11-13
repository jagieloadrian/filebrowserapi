package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.TrashItem
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class TrashRepository(
    private val db: Database,
) {
    fun insert(
        owner: String,
        item: TrashItem,
    ) {
        transaction(db) {
            TrashItemTable.insert {
                it[id] = item.id
                it[this.owner] = owner
                it[originalPath] = item.path
                it[directory] = item.directory
                it[size] = item.size
                it[deletedAt] = item.deletedAt.utc()
            }
        }
    }

    fun list(owner: String): List<TrashItem> =
        transaction(db) {
            TrashItemTable
                .selectAll()
                .where { TrashItemTable.owner eq owner }
                .orderBy(TrashItemTable.deletedAt, SortOrder.DESC)
                .map { it.toItem() }
        }

    fun find(
        owner: String,
        id: UUID,
    ): TrashItem? =
        transaction(db) {
            TrashItemTable
                .selectAll()
                .where { (TrashItemTable.owner eq owner) and (TrashItemTable.id eq id) }
                .singleOrNull()
                ?.toItem()
        }

    fun delete(
        owner: String,
        id: UUID,
    ) {
        transaction(db) { TrashItemTable.deleteWhere { (this.owner eq owner) and (this.id eq id) } }
    }

    fun findExpired(before: Instant): List<Pair<String, UUID>> =
        transaction(db) {
            TrashItemTable
                .select(TrashItemTable.owner, TrashItemTable.id)
                .where { TrashItemTable.deletedAt less before.utc() }
                .map { it[TrashItemTable.owner] to it[TrashItemTable.id] }
        }

    private fun ResultRow.toItem() =
        TrashItem(
            this[TrashItemTable.id],
            this[TrashItemTable.originalPath],
            this[TrashItemTable.directory],
            this[TrashItemTable.size],
            this[TrashItemTable.deletedAt].toInstant(),
        )
}
