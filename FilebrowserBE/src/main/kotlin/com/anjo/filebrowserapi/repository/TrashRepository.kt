package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.TrashItem
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

private const val COLUMNS = "id, original_path, directory, size, deleted_at"

@Repository
class TrashRepository(
    private val jdbc: JdbcClient,
) {
    fun insert(
        owner: String,
        item: TrashItem,
    ) {
        jdbc
            .sql(
                "insert into trash_item (id, owner, original_path, directory, size, deleted_at) values (:id, :owner, :path, :directory, :size, :deletedAt)",
            ).param("id", item.id)
            .param("owner", owner)
            .param("path", item.path)
            .param("directory", item.directory)
            .param("size", item.size)
            .param("deletedAt", OffsetDateTime.ofInstant(item.deletedAt, ZoneOffset.UTC))
            .update()
    }

    fun list(owner: String): List<TrashItem> =
        jdbc
            .sql("select $COLUMNS from trash_item where owner = :owner order by deleted_at desc")
            .param("owner", owner)
            .query { rs, _ -> rs.toItem() }
            .list()

    fun find(
        owner: String,
        id: UUID,
    ): TrashItem? =
        jdbc
            .sql("select $COLUMNS from trash_item where owner = :owner and id = :id")
            .param("owner", owner)
            .param("id", id)
            .query { rs, _ -> rs.toItem() }
            .optional()
            .orElse(null)

    fun delete(
        owner: String,
        id: UUID,
    ) {
        jdbc
            .sql("delete from trash_item where owner = :owner and id = :id")
            .param("owner", owner)
            .param("id", id)
            .update()
    }

    fun findExpired(before: Instant): List<Pair<String, UUID>> =
        jdbc
            .sql("select owner, id from trash_item where deleted_at < :before")
            .param("before", OffsetDateTime.ofInstant(before, ZoneOffset.UTC))
            .query { rs, _ -> rs.getString("owner") to rs.getObject("id", UUID::class.java) }
            .list()

    private fun ResultSet.toItem() =
        TrashItem(
            getObject("id", UUID::class.java),
            getString("original_path"),
            getBoolean("directory"),
            getLong("size"),
            getObject("deleted_at", OffsetDateTime::class.java).toInstant(),
        )
}
