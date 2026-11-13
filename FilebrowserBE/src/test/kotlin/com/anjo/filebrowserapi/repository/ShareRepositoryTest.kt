package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.Share
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
@Import(ShareRepository::class, MetadataRepository::class)
class ShareRepositoryTest(
    @Autowired private val repository: ShareRepository,
    @Autowired private val metadata: MetadataRepository,
) {
    private val now = Instant.parse("2026-11-13T12:00:00Z")
    private val older = Share(UUID.randomUUID(), "/a/old.txt", now.minusSeconds(60), now.plusSeconds(60))
    private val newer = Share(UUID.randomUUID(), "/a/new.txt", now, now.plusSeconds(120))
    private val expired = Share(UUID.randomUUID(), "/gone.txt", now.minusSeconds(120), now)

    @Test
    fun `should list own shares newest first and find only active tokens`() {
        // given
        repository.insert("user-1", older, "h1")
        repository.insert("user-1", newer, "h2")
        repository.insert("user-1", expired, "h3")
        repository.insert("user-2", Share(UUID.randomUUID(), "/b.txt", now, now.plusSeconds(60)), "h4")

        // when & then
        repository.list("user-1").map { it.id } shouldBe listOf(newer.id, older.id, expired.id)
        repository.findActive("h1", now) shouldBe ("user-1" to "/a/old.txt")
        repository.findActive("h3", now) shouldBe null
        repository.findActive("unknown", now) shouldBe null
    }

    @Test
    fun `should delete own share and expired shares only`() {
        // given
        repository.insert("user-1", older, "h1")
        repository.insert("user-1", expired, "h3")

        // when & then
        repository.delete("user-2", older.id) shouldBe false
        repository.deleteExpired(now) shouldBe 1
        repository.delete("user-1", older.id) shouldBe true
        repository.list("user-1") shouldBe emptyList()
    }

    @Test
    fun `should follow its file when the parent directory is moved`() {
        // given
        repository.insert("user-1", older, "h1")

        // when
        metadata.relocate("user-1", "/a", "/z")

        // then
        repository.findActive("h1", now) shouldBe ("user-1" to "/z/old.txt")
    }
}
