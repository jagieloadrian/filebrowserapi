package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.USER
import com.anjo.filebrowserapi.TestObjects.signIn
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.FileVersion
import com.anjo.filebrowserapi.repository.VersionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.security.core.context.SecurityContextHolder
import java.io.File
import java.time.Instant
import java.util.UUID

class VersionManagerTest :
    FunSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val workDir = tempdir()
        val repository = mockk<VersionRepository>(relaxed = true)
        val service = VersionManager(PathModifierUtils(AppConfiguration(workDir.path)), repository)
        val file = File("$workDir/$BASE_FOLDER/a.txt")

        fun stored(id: UUID) = File("$workDir/filebrowser-versions/$USER/$id")

        beforeEach {
            signIn()
            every { repository.find(any(), any()) } returns null
            every { repository.list(any(), any()) } returns emptyList()
        }

        afterEach { SecurityContextHolder.clearContext() }

        test("should move current content into the version store and prune beyond ten versions") {
            // given
            file.apply { parentFile.mkdirs() }.writeText("v1")
            val kept = slot<FileVersion>()
            every { repository.insert(USER, capture(kept)) } returns Unit
            val history = (1..11).map { FileVersion(UUID.randomUUID(), "/a.txt", 1, Instant.ofEpochSecond(100L - it)) }
            every { repository.list(USER, "/a.txt") } returns history

            // when
            service.keep(file)

            // then
            file.exists() shouldBe false
            kept.captured.path shouldBe "/a.txt"
            stored(kept.captured.id).readText() shouldBe "v1"
            verify(exactly = 1) { repository.delete(USER, history.last().id) }
        }

        test("should keep current content and bring the version back when restore is called") {
            // given
            val version = FileVersion(UUID.randomUUID(), "/a.txt", 2, Instant.EPOCH)
            every { repository.find(USER, version.id) } returns version
            file.apply { parentFile.mkdirs() }.writeText("current")
            stored(version.id).apply { parentFile.mkdirs() }.writeText("old")

            // when
            val result = service.restore(version.id.toString())

            // then
            result shouldBe "/a.txt"
            file.readText() shouldBe "old"
            verify(exactly = 1) { repository.insert(USER, match { it.path == "/a.txt" }) }
            verify(exactly = 1) { repository.delete(USER, version.id) }
        }

        test("should throw for bad and unknown version ids") {
            // when & then
            shouldThrow<BadInputException> { service.restore("nope") }
            shouldThrow<NotFoundException> { service.restore(UUID.randomUUID().toString()) }
        }

        test("should list versions of a file and delete stored files under a key") {
            // given
            val id = UUID.randomUUID()
            stored(id).apply { parentFile.mkdirs() }.writeText("x")
            every { repository.list(USER, "/a.txt") } returns listOf(FileVersion(id, "/a.txt", 1, Instant.EPOCH))
            every { repository.idsUnder(USER, "trash:t") } returns listOf(id)

            // when
            val versions = service.list("a.txt")
            service.deleteFiles(USER, "trash:t")

            // then
            versions.map { it.id } shouldBe listOf(id)
            stored(id).exists() shouldBe false
        }
    })
