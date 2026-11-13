package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.TestObjects.USER
import com.anjo.filebrowserapi.TestObjects.signIn
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.AlreadyExistsException
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.TrashItem
import com.anjo.filebrowserapi.repository.MetadataRepository
import com.anjo.filebrowserapi.repository.TrashRepository
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

class TrashManagerTest :
    FunSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val workDir = tempdir()
        val metadata = mockk<MetadataRepository>(relaxed = true)
        val repository = mockk<TrashRepository>(relaxed = true)
        val versions = mockk<VersionManager>(relaxed = true)
        val service = TrashManager(PathModifierUtils(AppConfiguration(workDir.path)), metadata, repository, versions)
        val item = TrashItem(UUID.randomUUID(), "/docs/a.txt", false, 11, Instant.EPOCH)
        val trashed = File("$workDir/filebrowser-trash/$USER/${item.id}")

        beforeEach {
            signIn()
            every { repository.find(any(), any()) } returns null
            every { repository.find(USER, item.id) } returns item
        }

        afterEach { SecurityContextHolder.clearContext() }

        test("should move entry into the trash store and record it when moveToTrash is called") {
            // given
            val file = File("$workDir/$BASE_FOLDER/docs/a.txt").apply { parentFile.mkdirs() }
            file.writeText(TEST_MESSAGE)
            val recorded = slot<TrashItem>()
            every { repository.insert(USER, capture(recorded)) } returns Unit

            // when
            service.moveToTrash(file)

            // then
            file.exists() shouldBe false
            recorded.captured.path shouldBe "/docs/a.txt"
            recorded.captured.size shouldBe TEST_MESSAGE.length.toLong()
            File("$workDir/filebrowser-trash/$USER/${recorded.captured.id}").readText() shouldBe TEST_MESSAGE
            verify(exactly = 1) { metadata.relocate(USER, "/docs/a.txt", "trash:${recorded.captured.id}") }
        }

        test("should put entry back with its metadata when restore is called") {
            // given
            trashed.apply { parentFile.mkdirs() }.writeText(TEST_MESSAGE)

            // when
            val result = service.restore(item.id.toString())

            // then
            result shouldBe "/docs/a.txt"
            File("$workDir/$BASE_FOLDER/docs/a.txt").readText() shouldBe TEST_MESSAGE
            verify(exactly = 1) { metadata.relocate(USER, "trash:${item.id}", "/docs/a.txt") }
            verify(exactly = 1) { repository.delete(USER, item.id) }
        }

        test("should throw AlreadyExistsException when original path is taken again") {
            // given
            File("$workDir/$BASE_FOLDER/docs/a.txt").apply { parentFile.mkdirs() }.writeText("new")

            // when & then
            shouldThrow<AlreadyExistsException> { service.restore(item.id.toString()) }
        }

        test("should throw BadInputException and NotFoundException for bad and unknown ids") {
            // when & then
            shouldThrow<BadInputException> { service.restore("not-a-uuid") }
            shouldThrow<NotFoundException> { service.purge(UUID.randomUUID().toString()) }
        }

        test("should delete content, metadata and record when an item is purged") {
            // given
            trashed.apply { parentFile.mkdirs() }.writeText(TEST_MESSAGE)

            // when
            service.purge(item.id.toString())

            // then
            trashed.exists() shouldBe false
            verify(exactly = 1) { versions.deleteFiles(USER, "trash:${item.id}") }
            verify(exactly = 1) { metadata.forget(USER, "trash:${item.id}") }
            verify(exactly = 1) { repository.delete(USER, item.id) }
        }

        test("should purge every own item when trash is emptied and expired items of all users on schedule") {
            // given
            val expired = UUID.randomUUID()
            every { repository.list(USER) } returns listOf(item)
            every { repository.findExpired(any()) } returns listOf("user-2" to expired)

            // when
            service.empty()
            service.purgeExpired()

            // then
            verify(exactly = 1) { repository.delete(USER, item.id) }
            verify(exactly = 1) { metadata.forget("user-2", "trash:$expired") }
        }
    })
