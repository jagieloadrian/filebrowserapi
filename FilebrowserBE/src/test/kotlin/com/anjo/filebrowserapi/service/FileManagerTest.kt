package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.PATH_TO_BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.PATH_TO_RESOURCES
import com.anjo.filebrowserapi.TestObjects.SECOND_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.CannotRealizeException
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class FileManagerTest {
    private val appConfiguration = AppConfiguration(path = "${System.getProperty("user.dir")}$PATH_TO_RESOURCES")

    private val modifierUtils = PathModifierUtils(appConfiguration)

    @InjectMockKs
    private lateinit var service: FileManager

    companion object {
        @JvmStatic
        @AfterAll
        fun clearAfterTests() {
            val baseDirectory = File(PATH_TO_BASE_FOLDER)
            baseDirectory.deleteRecursively()
        }
    }

    @AfterTest
    fun cleanUp() {
        val files =
            File("${System.getProperty("user.dir")}${PATH_TO_RESOURCES}/${BASE_FOLDER}")
                .walk()
                .map { file ->
                    file.path
                }.drop(1)
                .sortedDescending()
                .toList()

        files.forEach { s ->
            val temp = File(s)
            temp.deleteRecursively()
        }
    }

    @Test
    fun `given base directory with 2 files and null path when show files tree then return properly list`() {
        createFile(FIRST_FILE_NAME)
        createFile(SECOND_FILE_NAME)

        val expected = listOf("/${FIRST_FILE_NAME}", "/${SECOND_FILE_NAME}")

        // when
        val result = service.showFilesTree(null)

        // then
        result shouldContainExactly expected
    }

    @Test
    fun `given empty base directory and null path when show files tree then return empty list`() {
        // when
        val result = service.showFilesTree(null)

        // then
        result shouldHaveSize 0
    }

    @Test
    fun `given base directory with few folders and path when show files tree from specific path then return properly list`() {
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
    fun `given file and path null when create file then return path`() {
        // given
        val file = createFile(FIRST_FILE_NAME)

        val multipartFile: MultipartFile = MockMultipartFile("file", FIRST_FILE_NAME, "text/plain", file.inputStream())
        val expected = "/${FIRST_FILE_NAME}"

        // when
        val result = service.uploadFile(multipartFile, null)

        // then
        assertSoftly {
            result shouldBe expected
        }
    }

    @Test
    fun `given file name and wrong path when create file then return CannotRealizeException`() {
        // given
        val wrongPath = "/${SECOND_NAME}"
        val file = createFile(FIRST_FILE_NAME)
        val multipartFile: MultipartFile = MockMultipartFile("file", FIRST_FILE_NAME, "text/plain", file.inputStream())
        val expectedMessage = "Path: $wrongPath to create file or directory is not exist"

        // when
        val exception = shouldThrow<CannotRealizeException> { service.uploadFile(multipartFile, wrongPath) }

        // then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `given file name when getFile then return file`() {
        // given
        val expect = createFile(FIRST_FILE_NAME)

        // when
        val result = service.getFile(FIRST_FILE_NAME, null)

        // then
        assertSoftly {
            result.count() shouldBe expect.readBytes().count()
            result shouldBe expect.readBytes()
        }
    }

    @Test
    fun `given file name and wrong path when getFile then throw CannotRealizeException`() {
        // given
        val wrongPath = FIRST_NAME
        val expectedMessage = "Path: $FIRST_NAME to read file or directory is not exist"

        // when
        val exception = shouldThrow<CannotRealizeException> { service.getFile(FIRST_FILE_NAME, wrongPath) }

        // then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `given non existing file name without path when delete file then throw CannotRealizeException`() {
        // given
        val expectedMessage = "Cannot remove non existing file"

        // when
        val exception = shouldThrow<BadInputException> { service.deleteFile(FIRST_FILE_NAME, null) }

        // then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `given non existing file name with path when delete file then throw CannotRealizeException`() {
        // given
        val wrongPath = FIRST_NAME
        val expectedMessage = "Path: $FIRST_NAME to delete file or directory is not exist"

        // when
        val exception = shouldThrow<CannotRealizeException> { service.deleteFile(FIRST_FILE_NAME, wrongPath) }

        // then
        exception.message shouldBe expectedMessage
    }

    private fun createFile(fileName: String): File {
        val file = File("$PATH_TO_BASE_FOLDER/$fileName")
        file.createNewFile()
        println(file.name)
        return file
    }

    private fun createFolder(path: String): File {
        val tempFolder = File("$PATH_TO_BASE_FOLDER/$path")
        tempFolder.mkdir()
        return tempFolder
    }
}
