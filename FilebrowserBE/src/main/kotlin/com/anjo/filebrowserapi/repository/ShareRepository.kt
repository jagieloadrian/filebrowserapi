package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.Share
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class ShareRepository(
    private val jdbc: JdbcClient,
) {
    fun insert(
        owner: String,
        share: Share,
        tokenHash: String,
    ) {
        jdbc
            .sql(
                "insert into share_link (id, owner, path, token_hash, created_at, expires_at) " +
                    "values (:id, :owner, :path, :tokenHash, :createdAt, :expiresAt)",
            ).param("id", share.id)
            .param("owner", owner)
            .param("path", share.path)
            .param("tokenHash", tokenHash)
            .param("createdAt", share.createdAt.utc())
            .param("expiresAt", share.expiresAt.utc())
            .update()
    }

    fun list(owner: String): List<Share> =
        jdbc
            .sql("select id, path, created_at, expires_at from share_link where owner = :owner order by created_at desc, id")
            .param("owner", owner)
            .query { rs, _ ->
                Share(
                    rs.getObject("id", UUID::class.java),
                    rs.getString("path"),
                    rs.getObject("created_at", OffsetDateTime::class.java).toInstant(),
                    rs.getObject("expires_at", OffsetDateTime::class.java).toInstant(),
                )
            }.list()

    fun findActive(
        tokenHash: String,
        now: Instant,
    ): Pair<String, String>? =
        jdbc
            .sql("select owner, path from share_link where token_hash = :tokenHash and expires_at > :now")
            .param("tokenHash", tokenHash)
            .param("now", now.utc())
            .query { rs, _ -> rs.getString("owner") to rs.getString("path") }
            .optional()
            .orElse(null)

    fun delete(
        owner: String,
        id: UUID,
    ): Boolean =
        jdbc
            .sql("delete from share_link where owner = :owner and id = :id")
            .param("owner", owner)
            .param("id", id)
            .update() > 0

    fun deleteExpired(now: Instant): Int =
        jdbc
            .sql("delete from share_link where expires_at <= :now")
            .param("now", now.utc())
            .update()

    private fun Instant.utc() = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)
}
