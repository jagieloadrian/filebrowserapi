package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.EntryMeta
import com.anjo.filebrowserapi.model.FileHash
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

private val PATH_TABLES = listOf("entry_meta", "file_hash", "file_version")

@Repository
class MetadataRepository(
    private val jdbc: JdbcClient,
    private val batch: NamedParameterJdbcTemplate,
) {
    fun findUnder(
        owner: String,
        path: String,
    ): Map<String, EntryMeta> =
        under("select path, description, tags from entry_meta where $UNDER", owner, path)
            .query { rs, _ ->
                EntryMeta(rs.getString("path"), rs.getString("description"), (rs.getArray("tags").array as Array<*>).map { it as String })
            }.list()
            .associateBy { it.path }

    fun save(
        owner: String,
        path: String,
        description: String?,
        tags: List<String>,
    ) {
        if (description == null && tags.isEmpty()) {
            jdbc
                .sql("delete from entry_meta where owner = :owner and path = :path")
                .param("owner", owner)
                .param("path", path)
                .update()
            return
        }
        jdbc
            .sql(
                """
                insert into entry_meta (owner, path, description, tags)
                values (:owner, :path, :description, string_to_array(:tags, ','))
                on conflict (owner, path) do update set description = excluded.description, tags = excluded.tags
                """.trimIndent(),
            ).param("owner", owner)
            .param("path", path)
            .param("description", description)
            .param("tags", tags.joinToString(","))
            .update()
    }

    fun relocate(
        owner: String,
        from: String,
        to: String,
    ) {
        forget(owner, to)
        PATH_TABLES.forEach { table ->
            under("update $table set path = :to || substr(path, :cut) where $UNDER", owner, from)
                .param("to", to)
                .param("cut", from.length + 1)
                .update()
        }
    }

    fun copy(
        owner: String,
        from: String,
        to: String,
    ) {
        forget(owner, to)
        under(
            """
            insert into entry_meta (owner, path, description, tags)
            select owner, :to || substr(path, :cut), description, tags from entry_meta where $UNDER
            """.trimIndent(),
            owner,
            from,
        ).param("to", to)
            .param("cut", from.length + 1)
            .update()
    }

    fun findHashes(
        owner: String,
        path: String,
    ): Map<String, FileHash> =
        under("select path, size, modified_ms, sha256 from file_hash where $UNDER", owner, path)
            .query { rs, _ -> FileHash(rs.getString("path"), rs.getLong("size"), rs.getLong("modified_ms"), rs.getString("sha256")) }
            .list()
            .associateBy { it.path }

    fun saveHashes(
        owner: String,
        hashes: List<FileHash>,
    ) {
        if (hashes.isEmpty()) return
        batch.batchUpdate(
            """
            insert into file_hash (owner, path, size, modified_ms, sha256) values (:owner, :path, :size, :modified, :sha256)
            on conflict (owner, path) do update set size = excluded.size, modified_ms = excluded.modified_ms, sha256 = excluded.sha256
            """.trimIndent(),
            hashes
                .map {
                    MapSqlParameterSource()
                        .addValue("owner", owner)
                        .addValue("path", it.path)
                        .addValue("size", it.size)
                        .addValue("modified", it.modifiedMs)
                        .addValue("sha256", it.sha256)
                }.toTypedArray(),
        )
    }

    fun forget(
        owner: String,
        path: String,
    ) {
        PATH_TABLES.forEach { table -> under("delete from $table where $UNDER", owner, path).update() }
    }

    private fun under(
        sql: String,
        owner: String,
        path: String,
    ) = jdbc
        .sql(sql)
        .param("owner", owner)
        .param("path", path)
        .param("prefix", likePrefix(path))
}
