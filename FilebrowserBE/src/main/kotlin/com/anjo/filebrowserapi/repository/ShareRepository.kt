package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.Share
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.lessEq
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
class ShareRepository(
    private val db: Database,
) {
    fun insert(
        owner: String,
        share: Share,
        tokenHash: String,
    ) {
        transaction(db) {
            ShareLinkTable.insert {
                it[id] = share.id
                it[this.owner] = owner
                it[path] = share.path
                it[this.tokenHash] = tokenHash
                it[createdAt] = share.createdAt.utc()
                it[expiresAt] = share.expiresAt.utc()
            }
        }
    }

    fun list(owner: String): List<Share> =
        transaction(db) {
            ShareLinkTable
                .selectAll()
                .where { ShareLinkTable.owner eq owner }
                .orderBy(ShareLinkTable.createdAt to SortOrder.DESC, ShareLinkTable.id to SortOrder.ASC)
                .map {
                    Share(
                        it[ShareLinkTable.id],
                        it[ShareLinkTable.path],
                        it[ShareLinkTable.createdAt].toInstant(),
                        it[ShareLinkTable.expiresAt].toInstant(),
                    )
                }
        }

    fun findActive(
        tokenHash: String,
        now: Instant,
    ): Pair<String, String>? =
        transaction(db) {
            ShareLinkTable
                .select(ShareLinkTable.owner, ShareLinkTable.path)
                .where { (ShareLinkTable.tokenHash eq tokenHash) and (ShareLinkTable.expiresAt greater now.utc()) }
                .singleOrNull()
                ?.let { it[ShareLinkTable.owner] to it[ShareLinkTable.path] }
        }

    fun delete(
        owner: String,
        id: UUID,
    ): Boolean = transaction(db) { ShareLinkTable.deleteWhere { (this.owner eq owner) and (this.id eq id) } > 0 }

    fun deleteExpired(now: Instant): Int = transaction(db) { ShareLinkTable.deleteWhere { expiresAt lessEq now.utc() } }
}
