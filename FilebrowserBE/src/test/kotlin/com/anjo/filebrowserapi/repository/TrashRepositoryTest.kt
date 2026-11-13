package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.TrashItem
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
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
@Import(TrashRepository::class)
class TrashRepositoryTest(
    @Autowired private val repository: TrashRepository,
) {
    private val old = TrashItem(UUID.randomUUID(), "/old.txt", false, 3, Instant.parse("2026-01-01T00:00:00Z"))
    private val recent = TrashItem(UUID.randomUUID(), "/docs", true, 0, Instant.parse("2026-11-01T00:00:00Z"))

    @Test
    fun `should list own items newest first and find one by id`() {
        // given
        repository.insert("user-1", old)
        repository.insert("user-1", recent)
        repository.insert("user-2", TrashItem(UUID.randomUUID(), "/x", false, 1, Instant.now()))

        // when & then
        repository.list("user-1") shouldBe listOf(recent, old)
        repository.find("user-1", old.id) shouldBe old
        repository.find("user-2", old.id) shouldBe null
    }

    @Test
    fun `should return only expired items and forget deleted ones`() {
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
}
