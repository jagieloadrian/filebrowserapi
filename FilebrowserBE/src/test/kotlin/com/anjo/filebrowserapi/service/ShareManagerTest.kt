package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.USER
import com.anjo.filebrowserapi.TestObjects.signIn
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.Share
import com.anjo.filebrowserapi.repository.ShareRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.security.core.context.SecurityContextHolder
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat
import kotlin.test.Test

class ShareManagerTest {
    @TempDir
    private lateinit var workDir: Path

    private val repository = mockk<ShareRepository>(relaxed = true)
    private val service by lazy { ShareManager(PathModifierUtils(AppConfiguration(workDir.toString())), repository) }
    private val file: File get() = File("$workDir/$BASE_FOLDER/docs/a.txt")

    @BeforeEach
    fun signInUser() {
        signIn()
        file.apply { parentFile.mkdirs() }.writeText("shared")
        every { repository.findActive(any(), any()) } returns null
    }

    @AfterEach
    fun signOut() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should store only the token hash and expire after seven days by default`() {
        // given
        val share = slot<Share>()
        val hash = slot<String>()
        every { repository.insert(USER, capture(share), capture(hash)) } returns Unit

        // when
        val created = service.create("docs/a.txt", null)

        // then
        created.token shouldMatch Regex("[A-Za-z0-9_-]{43}")
        hash.captured shouldBe sha256(created.token)
        share.captured shouldBe created.share
        created.share.path shouldBe "/docs/a.txt"
        Duration.between(created.share.createdAt, created.share.expiresAt) shouldBe Duration.ofDays(7)
    }

    @ParameterizedTest
    @ValueSource(longs = [0, 31])
    fun `should throw BadInputException when days are out of range`(days: Long) {
        // when & then
        shouldThrow<BadInputException> { service.create("docs/a.txt", days) }
    }

    @Test
    fun `should share files only`() {
        // when & then
        shouldThrow<BadInputException> { service.create("docs", 1) }
        shouldThrow<NotFoundException> { service.create("docs/missing.txt", 1) }
    }

    @Test
    fun `should open the owner's file when token is active`() {
        // given
        every { repository.findActive(sha256("token"), any()) } returns (USER to "/docs/a.txt")

        // when & then
        service.open("token").readText() shouldBe "shared"
    }

    @Test
    fun `should refuse a stored path that leaves the owner's directory`() {
        // given
        every { repository.findActive(sha256("token"), any()) } returns (USER to "/../../etc/passwd")

        // when & then
        shouldThrow<BadInputException> { service.open("token") }
    }

    @ParameterizedTest
    @ValueSource(strings = ["trash:1", "/docs/missing.txt", "/docs"])
    fun `should throw NotFoundException when shared path is not a reachable file`(path: String) {
        // given
        every { repository.findActive(sha256("token"), any()) } returns (USER to path)

        // when & then
        shouldThrow<NotFoundException> { service.open("token") }
    }

    @Test
    fun `should throw NotFoundException when token is unknown or expired`() {
        // when & then
        shouldThrow<NotFoundException> { service.open("token") }
    }

    @Test
    fun `should revoke own share and reject bad or unknown ids`() {
        // given
        val id = java.util.UUID.randomUUID()
        every { repository.delete(USER, id) } returns true
        every { repository.delete(USER, neq(id)) } returns false

        // when
        service.revoke(id.toString())

        // then
        verify(exactly = 1) { repository.delete(USER, id) }
        shouldThrow<BadInputException> { service.revoke("nope") }
        shouldThrow<NotFoundException> {
            service.revoke(
                java.util.UUID
                    .randomUUID()
                    .toString(),
            )
        }
    }

    private fun sha256(token: String) = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))
}
