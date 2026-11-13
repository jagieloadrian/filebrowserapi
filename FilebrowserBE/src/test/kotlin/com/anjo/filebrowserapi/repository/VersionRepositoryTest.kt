package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.FileVersion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class VersionRepositoryTest :
    FunSpec({
        val repository = VersionRepository(TestDatabase.db)
        val older = FileVersion(UUID.randomUUID(), "/docs/a.txt", 1, Instant.parse("2026-11-01T00:00:00Z"))
        val newer = FileVersion(UUID.randomUUID(), "/docs/a.txt", 2, Instant.parse("2026-11-02T00:00:00Z"))
        val other = FileVersion(UUID.randomUUID(), "/docs_x/b.txt", 3, Instant.parse("2026-11-03T00:00:00Z"))

        beforeEach { TestDatabase.clean() }

        test("should list versions of one path newest first and find by id") {
            // given
            listOf(older, newer, other).forEach { repository.insert("user-1", it) }

            // when & then
            repository.list("user-1", "/docs/a.txt") shouldBe listOf(newer, older)
            repository.find("user-1", other.id) shouldBe other
            repository.find("user-2", other.id) shouldBe null
        }

        test("should return ids under a directory and delete one") {
            // given
            listOf(older, newer, other).forEach { repository.insert("user-1", it) }

            // when
            repository.delete("user-1", newer.id)

            // then
            repository.idsUnder("user-1", "/docs") shouldBe listOf(older.id)
        }
    })
