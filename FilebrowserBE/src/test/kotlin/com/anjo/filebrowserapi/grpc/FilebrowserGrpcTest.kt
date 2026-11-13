package com.anjo.filebrowserapi.grpc

import com.anjo.filebrowser.Filebrowser
import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.model.Entry
import com.anjo.filebrowserapi.service.DirectoryManager
import com.anjo.filebrowserapi.service.EntryManager
import com.anjo.filebrowserapi.service.FileManager
import com.google.protobuf.Empty
import com.google.protobuf.kotlin.toByteString
import io.grpc.Status
import io.grpc.StatusException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import org.springframework.util.unit.DataSize
import java.io.InputStream
import java.nio.file.Path
import java.time.Instant
import kotlin.random.Random
import kotlin.test.Test

class FilebrowserGrpcTest {
    private val directoryManager = mockk<DirectoryManager>()
    private val fileManager = mockk<FileManager>()
    private val entryManager = mockk<EntryManager>()
    private val service =
        FilebrowserGrpc(directoryManager, fileManager, entryManager, AppConfiguration(maxUploadSize = DataSize.ofBytes(16)))

    @TempDir
    private lateinit var workDir: Path

    @Test
    fun `should return files tree when showFilesTree is called`() =
        runTest {
            // given
            val expected = listOf("/$FIRST_NAME", "/$FIRST_NAME/$FIRST_FILE_NAME")
            val request = pathRequest(FIRST_NAME)
            every { fileManager.showFilesTree(FIRST_NAME) } returns expected

            // when
            val result = service.showFilesTree(request)

            // then
            result.directoriesList shouldBe expected
        }

    @Test
    fun `should store streamed chunks and return path when uploadFile is called`() =
        runTest {
            // given
            val expected = "/$FIRST_NAME/$FIRST_FILE_NAME"
            val head =
                Filebrowser.UploadFile
                    .newBuilder()
                    .setName(FIRST_FILE_NAME)
                    .setPath(FIRST_NAME)
                    .setOverwrite(true)
                    .setData("Test".toByteArray().toByteString())
                    .build()
            val tail =
                Filebrowser.UploadFile
                    .newBuilder()
                    .setData("Message".toByteArray().toByteString())
                    .build()
            var received = ""
            every { fileManager.uploadFile(FIRST_FILE_NAME, any(), FIRST_NAME, true) } answers {
                received = secondArg<InputStream>().readBytes().decodeToString()
                expected
            }

            // when
            val result = service.uploadFile(flowOf(head, tail))

            // then
            result.path shouldBe expected
            received shouldBe TEST_MESSAGE
        }

    @Test
    fun `should throw RESOURCE_EXHAUSTED when upload stream exceeds max upload size`() =
        runTest {
            // given
            val chunk =
                Filebrowser.UploadFile
                    .newBuilder()
                    .setName(FIRST_FILE_NAME)
                    .setData(ByteArray(10).toByteString())
                    .build()

            // when
            val result = shouldThrow<StatusException> { service.uploadFile(flowOf(chunk, chunk)) }

            // then
            result.status.code shouldBe Status.Code.RESOURCE_EXHAUSTED
            verify(exactly = 0) { fileManager.uploadFile(any(), any(), any(), any()) }
        }

    @Test
    fun `should throw BadInputException when upload stream is empty`() =
        runTest {
            // when & then
            shouldThrow<BadInputException> { service.uploadFile(emptyFlow()) }
        }

    @Test
    fun `should stream file in chunks when getFile is called`() =
        runTest {
            // given
            val expected = Random.nextBytes(CHUNK_SIZE * 2 + 1)
            every { fileManager.getFile(FIRST_FILE_NAME, FIRST_NAME) } returns
                workDir.resolve(FIRST_FILE_NAME).toFile().apply { writeBytes(expected) }

            // when
            val result = service.getFile(fileOperation(FIRST_FILE_NAME, FIRST_NAME)).toList()

            // then
            result.map { it.data.size() } shouldBe listOf(CHUNK_SIZE, CHUNK_SIZE, 1)
            result.fold(ByteArray(0)) { acc, chunk -> acc + chunk.data.toByteArray() } shouldBe expected
        }

    @Test
    fun `should return new path when renameFile is called`() =
        runTest {
            // given
            val expected = "/$SECOND_NAME"
            every { fileManager.renameFile(SECOND_NAME, FIRST_NAME) } returns expected

            // when
            val result = service.renameFile(fileOperation(SECOND_NAME, FIRST_NAME))

            // then
            result.path shouldBe expected
        }

    @Test
    fun `should delete file when deleteFile is called`() =
        runTest {
            // given
            every { fileManager.deleteFile(FIRST_FILE_NAME, FIRST_NAME) } just Runs

            // when
            val result = service.deleteFile(fileOperation(FIRST_FILE_NAME, FIRST_NAME))

            // then
            result shouldBe Empty.getDefaultInstance()
            verify(exactly = 1) { fileManager.deleteFile(FIRST_FILE_NAME, FIRST_NAME) }
        }

    @Test
    fun `should return directories tree from directory manager when showDirectoriesTree is called`() =
        runTest {
            // given
            val expected = listOf("/$FIRST_NAME/$SECOND_NAME")
            every { directoryManager.showDirectoriesTree(FIRST_NAME) } returns expected

            // when
            val result = service.showDirectoriesTree(pathRequest(FIRST_NAME))

            // then
            result.directoriesList shouldBe expected
        }

    @Test
    fun `should return directory path when addDirectory is called`() =
        runTest {
            // given
            val expected = "/$FIRST_NAME/$SECOND_NAME"
            every { directoryManager.addDirectory(SECOND_NAME, FIRST_NAME) } returns expected

            // when
            val result = service.addDirectory(fileOperation(SECOND_NAME, FIRST_NAME))

            // then
            result.path shouldBe expected
        }

    @Test
    fun `should return last directory path when addDirectoriesRecursively is called`() =
        runTest {
            // given
            val input = "$FIRST_NAME/$SECOND_NAME"
            every { directoryManager.addDirectoriesRecursively(input) } returns "/$input"

            // when
            val result = service.addDirectoriesRecursively(pathRequest(input))

            // then
            result.path shouldBe "/$input"
        }

    @Test
    fun `should return new path when renameDirectory is called`() =
        runTest {
            // given
            val expected = "/$SECOND_NAME"
            every { directoryManager.renameDirectory(SECOND_NAME, FIRST_NAME) } returns expected

            // when
            val result = service.renameDirectory(fileOperation(SECOND_NAME, FIRST_NAME))

            // then
            result.path shouldBe expected
        }

    @Test
    fun `should delete directory when deleteDirectory is called`() =
        runTest {
            // given
            every { directoryManager.deleteDirectory(FIRST_NAME) } just Runs

            // when
            val result = service.deleteDirectory(pathRequest(FIRST_NAME))

            // then
            result shouldBe Empty.getDefaultInstance()
            verify(exactly = 1) { directoryManager.deleteDirectory(FIRST_NAME) }
        }

    @Test
    fun `should return entries with metadata when listEntries is called`() =
        runTest {
            // given
            val modified = Instant.parse("2026-11-13T10:00:00.5Z")
            val request =
                Filebrowser.EntriesRequest
                    .newBuilder()
                    .setPath(FIRST_NAME)
                    .setName("first")
                    .setExtension("txt")
                    .build()
            every { entryManager.listEntries(FIRST_NAME, "first", "txt", "") } returns
                listOf(
                    Entry("/$FIRST_NAME", true, 0, modified, null),
                    Entry("/$FIRST_NAME/$FIRST_FILE_NAME", false, 4, modified, "text/plain"),
                )

            // when
            val result = service.listEntries(request).entriesList

            // then
            result.map { it.path } shouldBe listOf("/$FIRST_NAME", "/$FIRST_NAME/$FIRST_FILE_NAME")
            result[0].directory shouldBe true
            result[0].contentType shouldBe ""
            result[1].size shouldBe 4
            result[1].contentType shouldBe "text/plain"
            result[1].modified.seconds shouldBe modified.epochSecond
            result[1].modified.nanos shouldBe modified.nano
        }

    @Test
    fun `should return target path when copyEntry and moveEntry are called`() =
        runTest {
            // given
            val request =
                Filebrowser.EntryTransfer
                    .newBuilder()
                    .setSource(FIRST_NAME)
                    .setTarget(SECOND_NAME)
                    .build()
            every { entryManager.copyEntry(FIRST_NAME, SECOND_NAME) } returns "/$SECOND_NAME"
            every { entryManager.moveEntry(FIRST_NAME, SECOND_NAME) } returns "/$SECOND_NAME"

            // when & then
            service.copyEntry(request).path shouldBe "/$SECOND_NAME"
            service.moveEntry(request).path shouldBe "/$SECOND_NAME"
        }

    @Test
    fun `should delete directory with content when deleteDirectoryRecursively is called`() =
        runTest {
            // given
            every { directoryManager.deleteDirectoryRecursively(FIRST_NAME) } just Runs

            // when
            val result = service.deleteDirectoryRecursively(pathRequest(FIRST_NAME))

            // then
            result shouldBe Empty.getDefaultInstance()
            verify(exactly = 1) { directoryManager.deleteDirectoryRecursively(FIRST_NAME) }
        }

    @Test
    fun `should return updated entry when updateEntryMeta is called`() =
        runTest {
            // given
            val request =
                Filebrowser.EntryMetaUpdate
                    .newBuilder()
                    .setPath(FIRST_NAME)
                    .setDescription("notes")
                    .addTags("work")
                    .build()
            every { entryManager.updateMeta(FIRST_NAME, "notes", listOf("work")) } returns
                Entry("/$FIRST_NAME", true, 0, Instant.EPOCH, null, "notes", listOf("work"))

            // when
            val result = service.updateEntryMeta(request)

            // then
            result.description shouldBe "notes"
            result.tagsList shouldBe listOf("work")
        }

    private fun pathRequest(path: String) =
        Filebrowser.PathRequest
            .newBuilder()
            .setPath(path)
            .build()

    private fun fileOperation(
        name: String,
        path: String,
    ) = Filebrowser.FileOperation
        .newBuilder()
        .setName(name)
        .setPath(path)
        .build()
}
