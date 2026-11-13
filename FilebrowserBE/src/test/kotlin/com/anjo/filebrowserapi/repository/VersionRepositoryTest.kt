package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.FileVersion
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest
import org.springframework.context.annotation.Import
import java.time.Instant
import java.util.UUID
import kotlin.test.Test

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(VersionRepository::class)
class VersionRepositoryTest(
    @Autowired private val repository: VersionRepository,
) {
    private val older = FileVersion(UUID.randomUUID(), "/docs/a.txt", 1, Instant.parse("2026-11-01T00:00:00Z"))
    private val newer = FileVersion(UUID.randomUUID(), "/docs/a.txt", 2, Instant.parse("2026-11-02T00:00:00Z"))
    private val other = FileVersion(UUID.randomUUID(), "/docs_x/b.txt", 3, Instant.parse("2026-11-03T00:00:00Z"))

    @Test
    fun `should list versions of one path newest first and find by id`() {
        // given
        listOf(older, newer, other).forEach { repository.insert("user-1", it) }

        // when & then
        repository.list("user-1", "/docs/a.txt") shouldBe listOf(newer, older)
        repository.find("user-1", other.id) shouldBe other
        repository.find("user-2", other.id) shouldBe null
    }

    @Test
    fun `should return ids under a directory and delete one`() {
        // given
        listOf(older, newer, other).forEach { repository.insert("user-1", it) }

        // when
        repository.delete("user-1", newer.id)

        // then
        repository.idsUnder("user-1", "/docs") shouldBe listOf(older.id)
    }
}
