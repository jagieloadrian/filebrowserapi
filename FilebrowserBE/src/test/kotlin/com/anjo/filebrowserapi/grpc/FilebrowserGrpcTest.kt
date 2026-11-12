package com.anjo.filebrowserapi.grpc

import com.anjo.filebrowser.Filebrowser
import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.service.DirectoryManager
import com.anjo.filebrowserapi.service.FileManager
import com.google.protobuf.Empty
import com.google.protobuf.kotlin.toByteString
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class FilebrowserGrpcTest {
    private val directoryManager = mockk<DirectoryManager>()
    private val fileManager = mockk<FileManager>()
    private val service = FilebrowserGrpc(directoryManager, fileManager)

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
    fun `should store file and return its path when uploadFile is called`() =
        runTest {
            // given
            val content = TEST_MESSAGE.toByteArray()
            val expected = "/$FIRST_NAME/$FIRST_FILE_NAME"
            val request =
                Filebrowser.UploadFile
                    .newBuilder()
                    .setData(content.toByteString())
                    .setName(FIRST_FILE_NAME)
                    .setContentType("text/plain")
                    .setPath(FIRST_NAME)
                    .build()
            every { fileManager.uploadFile(any(), FIRST_NAME) } returns expected

            // when
            val result = service.uploadFile(request)

            // then
            result.path shouldBe expected
            verify(exactly = 1) {
                fileManager.uploadFile(
                    match { it.originalFilename == FIRST_FILE_NAME && it.bytes.contentEquals(content) },
                    FIRST_NAME,
                )
            }
        }

    @Test
    fun `should return file bytes when getFile is called`() =
        runTest {
            // given
            val expected = TEST_MESSAGE.toByteArray()
            every { fileManager.getFile(FIRST_FILE_NAME, FIRST_NAME) } returns expected

            // when
            val result = service.getFile(fileOperation(FIRST_FILE_NAME, FIRST_NAME))

            // then
            result.data.toByteArray() shouldBe expected
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
