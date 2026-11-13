package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.TrashItem
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class TrashRepositoryTest :
    FunSpec({
        val repository = TrashRepository(TestDatabase.db)
        val old = TrashItem(UUID.randomUUID(), "/old.txt", false, 3, Instant.parse("2026-01-01T00:00:00Z"))
        val recent = TrashItem(UUID.randomUUID(), "/docs", true, 0, Instant.parse("2026-11-01T00:00:00Z"))

        beforeEach { TestDatabase.clean() }

        test("should list own items newest first and find one by id") {
            // given
            repository.insert("user-1", old)
            repository.insert("user-1", recent)
            repository.insert("user-2", TrashItem(UUID.randomUUID(), "/x", false, 1, Instant.parse("2026-11-02T00:00:00Z")))

            // when & then
            repository.list("user-1") shouldBe listOf(recent, old)
            repository.find("user-1", old.id) shouldBe old
            repository.find("user-2", old.id) shouldBe null
        }

        test("should return only expired items and forget deleted ones") {
            // given
            repository.insert("user-1", old)
            repository.insert("user-1", recent)

            // when
            val expired = repository.findExpired(Instant.parse("2026-06-01T00:00:00Z"))
            repository.delete("user-1", recent.id)

            // then
            expired shouldContain ("user-1" to old.id)
            expired shouldNotContain ("user-1" to recent.id)
            repository.find("user-1", recent.id) shouldBe null
        }
    })
