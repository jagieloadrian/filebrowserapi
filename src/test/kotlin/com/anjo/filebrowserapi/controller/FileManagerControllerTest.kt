package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.TestObjects
import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.PATH_TO_BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.SECOND_FILE_NAME
import com.anjo.filebrowserapi.service.FileManagerService
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatusCode
import org.springframework.mock.web.MockMultipartFile
import java.io.File

@ExtendWith(MockKExtension::class)
class FileManagerControllerTest(
    @MockK
    private val service: FileManagerService
) {

    @InjectMockKs
    private lateinit var controller: FileManagerController

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUpClass() {
            val baseDirectory = File(PATH_TO_BASE_FOLDER)
            baseDirectory.mkdir()
        }

    }

    @Test
    fun `given request when getFilesTree then return list of string`() {
        //given
        val expected = listOf("/${FIRST_FILE_NAME}", "/${SECOND_FILE_NAME}")
        val expectedCode = 200

        every { service.showFilesTree(any()) } returns expected
        //when
        val response = controller.getFilesTree(null)

        //then
        assertSoftly {
            response.statusCode shouldBe HttpStatusCode.valueOf(expectedCode)
            response.body shouldContainExactly expected
        }
    }

    @Test
    fun `given request when uploadFile then return list of string`() {
        //given
        val expected = "/$FIRST_FILE_NAME"
        val expectedCode = 200
        val tempFile = createFile()

        val mockFile = MockMultipartFile(FIRST_FILE_NAME, tempFile.inputStream())

        every { service.uploadFile(any(), any()) } returns expected
        //when
        val response = controller.uploadFile(mockFile, null)

        //then
        assertSoftly {
            response.statusCode shouldBe HttpStatusCode.valueOf(expectedCode)
            response.body shouldBe expected
        }
    }


    @Test
    fun `given request when getFile then return file`() {
        //given
        val fileName = FIRST_FILE_NAME
        val expectedCode = 200
        val tempFile = createFile()

        val expectedFile = MockMultipartFile(fileName, tempFile.inputStream()).bytes

        every { service.getFile(any(), any()) } returns expectedFile
        //when
        val response = controller.getFile(fileName, null)

        //then
        assertSoftly {
            response.statusCode shouldBe HttpStatusCode.valueOf(expectedCode)
            response.body shouldBe expectedFile
        }
    }

    @Test
    fun `given path when deleteFile then return only statusCode`() {
        //given
        val expectedCode = 204

        every { service.deleteFile(any(), any()) } returns Unit
        //when
        val response = controller.deleteFile(FIRST_FILE_NAME, null)

        //then
        assertSoftly {
            response.statusCode shouldBe HttpStatusCode.valueOf(expectedCode)
        }
    }

    private fun createFile(): File {
        val file = File("${PATH_TO_BASE_FOLDER}/$FIRST_FILE_NAME")
        file.createNewFile()
        println(file.name)
        return file
    }

    @AfterEach
    fun cleanUp() {
        val files =
            File("${System.getProperty("user.dir")}${TestObjects.PATH_TO_RESOURCES}/${TestObjects.BASE_FOLDER}").walk()
                .map { file ->
                    file.path
                }
                .drop(1)
                .sortedDescending()
                .toList()

        files.forEach { s ->
            val temp = File(s)
            temp.deleteRecursively()
        }
    }
}