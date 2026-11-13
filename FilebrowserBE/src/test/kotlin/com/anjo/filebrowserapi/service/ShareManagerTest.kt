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
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.security.core.context.SecurityContextHolder
import java.io.File
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat

class ShareManagerTest :
    FunSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val workDir = tempdir()
        val repository = mockk<ShareRepository>(relaxed = true)
        val service = ShareManager(PathModifierUtils(AppConfiguration(workDir.path)), repository)
        val file = File("$workDir/$BASE_FOLDER/docs/a.txt")

        beforeEach {
            signIn()
            file.apply { parentFile.mkdirs() }.writeText("shared")
            every { repository.findActive(any(), any()) } returns null
        }

        afterEach { SecurityContextHolder.clearContext() }

        test("should store only the token hash and expire after seven days by default") {
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

        context("should throw BadInputException when days are out of range") {
            withTests(0L, 31L) { days ->
                // when & then
                shouldThrow<BadInputException> { service.create("docs/a.txt", days) }
            }
        }

        test("should share files only") {
            // when & then
            shouldThrow<BadInputException> { service.create("docs", 1) }
            shouldThrow<NotFoundException> { service.create("docs/missing.txt", 1) }
        }

        test("should open the owner's file when token is active") {
            // given
            every { repository.findActive(sha256("token"), any()) } returns (USER to "/docs/a.txt")

            // when & then
            service.open("token").readText() shouldBe "shared"
        }

        test("should refuse a stored path that leaves the owner's directory") {
            // given
            every { repository.findActive(sha256("token"), any()) } returns (USER to "/../../etc/passwd")

            // when & then
            shouldThrow<BadInputException> { service.open("token") }
        }

        context("should throw NotFoundException when shared path is not a reachable file") {
            withTests("trash:1", "/docs/missing.txt", "/docs") { path ->
                // given
                every { repository.findActive(sha256("token"), any()) } returns (USER to path)

                // when & then
                shouldThrow<NotFoundException> { service.open("token") }
            }
        }

        test("should throw NotFoundException when token is unknown or expired") {
            // when & then
            shouldThrow<NotFoundException> { service.open("token") }
        }

        test("should revoke own share and reject bad or unknown ids") {
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
    })

private fun sha256(token: String) = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))
