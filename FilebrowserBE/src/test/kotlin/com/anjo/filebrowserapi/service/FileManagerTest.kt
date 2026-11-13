package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.TestObjects.signIn
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.AlreadyExistsException
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.exception.QuotaExceededException
import com.anjo.filebrowserapi.repository.MetadataRepository
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.springframework.security.core.context.SecurityContextHolder
import java.io.File
import java.nio.file.Path
import kotlin.test.Test

class FileManagerTest {
    @TempDir
    private lateinit var workDir: Path

    private val baseFolder: String get() = "$workDir/$BASE_FOLDER"

    private val metadata = mockk<MetadataRepository>(relaxed = true)
    private val versions = mockk<VersionManager>(relaxed = true)
    private val quota = mockk<QuotaManager>(relaxed = true)

    private val utils by lazy { PathModifierUtils(AppConfiguration(workDir.toString())) }

    private val service by lazy {
        FileManager(utils, metadata, TrashManager(utils, metadata, mockk(relaxed = true), mockk(relaxed = true)), versions, quota)
    }

    @BeforeEach
    fun createBaseFolder() {
        File(baseFolder).mkdirs()
    }

    @BeforeEach
    fun signInUser() {
        signIn()
    }

    @AfterEach
    fun signOut() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should return sorted files when base directory has two files and path is null`() {
        createFile(FIRST_FILE_NAME)
        createFile(SECOND_FILE_NAME)

        val expected = listOf("/${FIRST_FILE_NAME}", "/${SECOND_FILE_NAME}")

        // when
        val result = service.showFilesTree(null)

        // then
        result shouldContainExactly expected
    }

    @Test
    fun `should return empty list when base directory is empty and path is null`() {
        // when
        val result = service.showFilesTree(null)

        // then
        result shouldHaveSize 0
    }

    @Test
    fun `should return files under path when path points to folder`() {
        createFolder(FIRST_NAME)
        createFile("${FIRST_NAME}/${SECOND_FILE_NAME}")
        createFile("${FIRST_NAME}/${FIRST_FILE_NAME}")

        val expected =
            listOf("/${FIRST_NAME}/${FIRST_FILE_NAME}", "/${FIRST_NAME}/${SECOND_FILE_NAME}")

        // when
        val result = service.showFilesTree(FIRST_NAME)

        // then
        result shouldContainExactly expected
    }

    @Test
    fun `should return file path when uploading file without path`() {
        // given
        val expected = "/${FIRST_FILE_NAME}"

        // when
        val result = service.uploadFile(FIRST_FILE_NAME, ByteArray(1).inputStream(), 1, null)

        // then
        assertSoftly {
            result shouldBe expected
        }
    }

    @Test
    fun `should throw NotFoundException when uploading file to missing path`() {
        // given
        val wrongPath = "/${SECOND_NAME}"
        val file = createFile(FIRST_FILE_NAME)
        val expectedMessage = "Path: $wrongPath to create file or directory is not exist"

        // when
        val exception = shouldThrow<NotFoundException> { service.uploadFile(FIRST_FILE_NAME, file.inputStream(), 0, wrongPath) }

        // then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `should throw AlreadyExistsException when uploaded file exists and overwrite is false`() {
        // given
        createFile(FIRST_FILE_NAME).writeText(TEST_MESSAGE)

        // when
        val exception = shouldThrow<AlreadyExistsException> { service.uploadFile(FIRST_FILE_NAME, "new".byteInputStream(), 3, null) }

        // then
        exception.message shouldBe "File $FIRST_FILE_NAME already exists"
        File("$baseFolder/$FIRST_FILE_NAME").readText() shouldBe TEST_MESSAGE
    }

    @Test
    fun `should replace file content when uploaded file exists and overwrite is true`() {
        // given
        createFile(FIRST_FILE_NAME).writeText(TEST_MESSAGE)

        // when
        val result = service.uploadFile(FIRST_FILE_NAME, "new".byteInputStream(), 3, null, overwrite = true)

        // then
        result shouldBe "/$FIRST_FILE_NAME"
        File("$baseFolder/$FIRST_FILE_NAME").readText() shouldBe "new"
        verify(exactly = 1) { versions.keep(File("$baseFolder/$FIRST_FILE_NAME")) }
    }

    @Test
    fun `should throw AlreadyExistsException when uploaded file name is a directory and overwrite is true`() {
        // given
        createFolder(FIRST_NAME)

        // when & then
        shouldThrow<AlreadyExistsException> { service.uploadFile(FIRST_NAME, "new".byteInputStream(), 3, null, overwrite = true) }
        File("$baseFolder/$FIRST_NAME").isDirectory shouldBe true
    }

    @Test
    fun `should return file content when file exists`() {
        // given
        createFile(FIRST_FILE_NAME).writeText(TEST_MESSAGE)

        // when
        val result = service.getFile(FIRST_FILE_NAME, null)

        // then
        result.readText() shouldBe TEST_MESSAGE
    }

    @Test
    fun `should throw NotFoundException when reading file from missing path`() {
        // given
        val wrongPath = FIRST_NAME
        val expectedMessage = "Path: $FIRST_NAME to read file or directory is not exist"

        // when
        val exception = shouldThrow<NotFoundException> { service.getFile(FIRST_FILE_NAME, wrongPath) }

        // then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `should throw NotFoundException when reading missing file`() {
        // when & then
        shouldThrow<NotFoundException> { service.getFile(FIRST_FILE_NAME, null) }
    }

    @Test
    fun `should throw BadInputException when reading a directory`() {
        // given
        createFolder(FIRST_NAME)

        // when & then
        shouldThrow<BadInputException> { service.getFile(FIRST_NAME, null) }
    }

    @Test
    fun `should throw NotFoundException when deleting missing file without path`() {
        // given
        val expectedMessage = "Cannot remove non existing file"

        // when
        val exception = shouldThrow<NotFoundException> { service.deleteFile(FIRST_FILE_NAME, null) }

        // then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `should throw NotFoundException when deleting file from missing path`() {
        // given
        val wrongPath = FIRST_NAME
        val expectedMessage = "Path: $FIRST_NAME to delete file or directory is not exist"

        // when
        val exception = shouldThrow<NotFoundException> { service.deleteFile(FIRST_FILE_NAME, wrongPath) }

        // then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `should return renamed path when file is renamed`() {
        // given
        createFile(FIRST_FILE_NAME)

        // when
        val result = service.renameFile(SECOND_FILE_NAME, FIRST_FILE_NAME)

        // then
        result shouldBe "/$SECOND_FILE_NAME"
        File("$baseFolder/$SECOND_FILE_NAME").exists() shouldBe true
        verify(exactly = 1) { metadata.relocate("user-1", "/$FIRST_FILE_NAME", "/$SECOND_FILE_NAME") }
    }

    @Test
    fun `should throw NotFoundException when renaming missing file`() {
        // when & then
        shouldThrow<NotFoundException> { service.renameFile(SECOND_FILE_NAME, FIRST_FILE_NAME) }
    }

    @Test
    fun `should throw BadInputException when renamed path is a directory`() {
        // given
        createFolder(FIRST_NAME)

        // when & then
        shouldThrow<BadInputException> { service.renameFile(SECOND_NAME, FIRST_NAME) }
    }

    @Test
    fun `should store file under generated txt name when uploaded file has no name`() {
        // given

        // when
        val result = service.uploadFile(null, "content".byteInputStream(), 7, null)

        // then
        result shouldMatch Regex("^/[0-9a-f-]{8}-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.txt$")
        File("$baseFolder$result").readText() shouldBe "content"
    }

    @Test
    fun `should not write the file when the quota is exceeded`() {
        // given
        every { quota.reserve(3) } throws QuotaExceededException("full")

        // when & then
        shouldThrow<QuotaExceededException> { service.uploadFile(FIRST_FILE_NAME, "new".byteInputStream(), 3, null) }
        File("$baseFolder/$FIRST_FILE_NAME").exists() shouldBe false
    }

    private fun createFile(fileName: String): File {
        val file = File("$baseFolder/$fileName")
        file.createNewFile()
        return file
    }

    private fun createFolder(path: String): File {
        val tempFolder = File("$baseFolder/$path")
        tempFolder.mkdir()
        return tempFolder
    }

    @Test
    fun `should throw BadInputException when showFilesTree leaves base directory`() {
        // when & then
        shouldThrow<BadInputException> { service.showFilesTree("../..") }
    }

    @Test
    fun `should throw BadInputException when uploadFile leaves base directory`() {
        // when & then
        shouldThrow<BadInputException> { service.uploadFile("../evil.txt", ByteArray(1).inputStream(), 1, null) }
    }

    @Test
    fun `should throw BadInputException when getFile leaves base directory`() {
        // when & then
        shouldThrow<BadInputException> { service.getFile("../$FIRST_FILE_NAME", null) }
    }

    @Test
    fun `should throw BadInputException when renameFile leaves base directory`() {
        // when & then
        shouldThrow<BadInputException> { service.renameFile("../../$SECOND_FILE_NAME", FIRST_FILE_NAME) }
    }

    @Test
    fun `should throw BadInputException when deleteFile leaves base directory`() {
        // when & then
        shouldThrow<BadInputException> { service.deleteFile("../$FIRST_FILE_NAME", null) }
    }
}
