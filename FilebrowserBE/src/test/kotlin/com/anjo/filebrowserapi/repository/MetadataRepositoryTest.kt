package com.anjo.filebrowserapi.repository

import com.anjo.filebrowserapi.model.FileHash
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest
import org.springframework.context.annotation.Import
import kotlin.test.Test

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MetadataRepository::class)
class MetadataRepositoryTest(
    @Autowired private val repository: MetadataRepository,
) {
    @Test
    fun `should return saved description and tags when entries below path are read`() {
        // given
        repository.save(OWNER, "/docs/a.txt", "notes", listOf("work", "draft"))
        repository.save(OTHER, "/docs/b.txt", "other user", emptyList())

        // when
        val result = repository.findUnder(OWNER, "/docs")

        // then
        result.keys shouldBe setOf("/docs/a.txt")
        result.getValue("/docs/a.txt").description shouldBe "notes"
        result.getValue("/docs/a.txt").tags shouldBe listOf("work", "draft")
    }

    @Test
    fun `should remove row when description and tags are cleared`() {
        // given
        repository.save(OWNER, "/a.txt", "notes", listOf("work"))

        // when
        repository.save(OWNER, "/a.txt", null, emptyList())

        // then
        repository.findUnder(OWNER, "").shouldBeEmpty()
    }

    @Test
    fun `should move directory rows but not siblings with the same prefix when relocate is called`() {
        // given
        repository.save(OWNER, "/a", "dir", emptyList())
        repository.save(OWNER, "/a/b.txt", "child", emptyList())
        repository.save(OWNER, "/ab.txt", "sibling", emptyList())

        // when
        repository.relocate(OWNER, "/a", "/z")

        // then
        repository.findUnder(OWNER, "").keys shouldBe setOf("/z", "/z/b.txt", "/ab.txt")
    }

    @Test
    fun `should treat LIKE wildcards in paths literally`() {
        // given
        repository.save(OWNER, "/a_%", "wild", emptyList())
        repository.save(OWNER, "/abc/x.txt", "plain", emptyList())

        // when
        repository.forget(OWNER, "/a_%")

        // then
        repository.findUnder(OWNER, "").keys shouldBe setOf("/abc/x.txt")
    }

    @Test
    fun `should copy rows and replace stale target rows when copy is called`() {
        // given
        repository.save(OWNER, "/a/b.txt", "child", listOf("x"))
        repository.save(OWNER, "/c/stale.txt", "stale", emptyList())

        // when
        repository.copy(OWNER, "/a", "/c")

        // then
        repository.findUnder(OWNER, "").keys shouldBe setOf("/a/b.txt", "/c/b.txt")
        repository.findUnder(OWNER, "/c").getValue("/c/b.txt").tags shouldBe listOf("x")
    }

    @Test
    fun `should store hashes and move them with their files`() {
        // given
        repository.saveHashes(OWNER, listOf(FileHash("/a/x.bin", 3, 10, "abc"), FileHash("/b.bin", 3, 11, "abc")))
        repository.saveHashes(OWNER, listOf(FileHash("/b.bin", 4, 12, "def")))

        // when
        repository.relocate(OWNER, "/a", "/z")

        // then
        repository.findHashes(OWNER, "") shouldBe
            mapOf("/z/x.bin" to FileHash("/z/x.bin", 3, 10, "abc"), "/b.bin" to FileHash("/b.bin", 4, 12, "def"))
    }

    private companion object {
        const val OWNER = "user-1"
        const val OTHER = "user-2"
    }
}
