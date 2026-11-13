package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.FileVersion
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class VersionRepository(
    private val jdbc: JdbcClient,
) {
    fun insert(
        owner: String,
        version: FileVersion,
    ) {
        jdbc
            .sql("insert into file_version (id, owner, path, size, created_at) values (:id, :owner, :path, :size, :createdAt)")
            .param("id", version.id)
            .param("owner", owner)
            .param("path", version.path)
            .param("size", version.size)
            .param("createdAt", OffsetDateTime.ofInstant(version.createdAt, ZoneOffset.UTC))
            .update()
    }

    fun list(
        owner: String,
        path: String,
    ): List<FileVersion> =
        jdbc
            .sql("select id, path, size, created_at from file_version where owner = :owner and path = :path order by created_at desc, id")
            .param("owner", owner)
            .param("path", path)
            .query { rs, _ -> rs.toVersion() }
            .list()

    fun find(
        owner: String,
        id: UUID,
    ): FileVersion? =
        jdbc
            .sql("select id, path, size, created_at from file_version where owner = :owner and id = :id")
            .param("owner", owner)
            .param("id", id)
            .query { rs, _ -> rs.toVersion() }
            .optional()
            .orElse(null)

    fun idsUnder(
        owner: String,
        path: String,
    ): List<UUID> =
        jdbc
            .sql("select id from file_version where $UNDER")
            .param("owner", owner)
            .param("path", path)
            .param("prefix", likePrefix(path))
            .query { rs, _ -> rs.getObject("id", UUID::class.java) }
            .list()

    fun delete(
        owner: String,
        id: UUID,
    ) {
        jdbc
            .sql("delete from file_version where owner = :owner and id = :id")
            .param("owner", owner)
            .param("id", id)
            .update()
    }

    private fun ResultSet.toVersion() =
        FileVersion(
            getObject("id", UUID::class.java),
            getString("path"),
            getLong("size"),
            getObject("created_at", OffsetDateTime::class.java).toInstant(),
        )
}
