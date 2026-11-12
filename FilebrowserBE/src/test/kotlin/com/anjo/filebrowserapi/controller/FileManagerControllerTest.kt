package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.service.FileManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.patch
import java.nio.file.Path
import kotlin.test.Test

@WebMvcTest(FileManagerController::class)
@ActiveProfiles("web")
class FileManagerControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean
    private lateinit var service: FileManager

    @TempDir
    private lateinit var workDir: Path

    @Test
    fun `should return files tree when GET is called`() {
        // given
        every { service.showFilesTree(FIRST_NAME) } returns listOf("/$FIRST_NAME/$FIRST_FILE_NAME")

        // when
        val response = mockMvc.get(FILES) { param("path", FIRST_NAME) }.andReturn().response

        // then
        response.status shouldBe 200
        response.contentAsString shouldBe """["/$FIRST_NAME/$FIRST_FILE_NAME"]"""
    }

    @Test
    fun `should return stored path when file is uploaded`() {
        // given
        val input = MockMultipartFile("uploadFile", FIRST_FILE_NAME, "text/plain", TEST_MESSAGE.toByteArray())
        every { service.uploadFile(FIRST_FILE_NAME, any(), FIRST_NAME) } returns "/$FIRST_NAME/$FIRST_FILE_NAME"

        // when
        val response =
            mockMvc
                .multipart(FILES) {
                    file(input)
                    param("path", FIRST_NAME)
                }.andReturn()
                .response

        // then
        response.status shouldBe 200
        response.contentAsString shouldBe "/$FIRST_NAME/$FIRST_FILE_NAME"
    }

    @Test
    fun `should pass overwrite flag to service when upload sets overwrite`() {
        // given
        val input = MockMultipartFile("uploadFile", FIRST_FILE_NAME, "text/plain", TEST_MESSAGE.toByteArray())
        every { service.uploadFile(FIRST_FILE_NAME, any(), null, true) } returns "/$FIRST_FILE_NAME"

        // when
        val response =
            mockMvc
                .multipart(FILES) {
                    file(input)
                    param("overwrite", "true")
                }.andReturn()
                .response

        // then
        response.status shouldBe 200
        verify(exactly = 1) { service.uploadFile(FIRST_FILE_NAME, any(), null, true) }
    }

    @Test
    fun `should return new path when file is renamed`() {
        // given
        every { service.renameFile(SECOND_FILE_NAME, "$FIRST_NAME/$FIRST_FILE_NAME") } returns "/$FIRST_NAME/$SECOND_FILE_NAME"

        // when
        val response =
            mockMvc
                .patch(FILES) {
                    param("sourcePath", "$FIRST_NAME/$FIRST_FILE_NAME")
                    param("newFileName", SECOND_FILE_NAME)
                }.andReturn()
                .response

        // then
        response.status shouldBe 200
        response.contentAsString shouldBe "/$FIRST_NAME/$SECOND_FILE_NAME"
    }

    @Test
    fun `should return 400 with details when new file name contains slash`() {
        // when
        val response =
            mockMvc
                .patch(FILES) {
                    param("sourcePath", FIRST_FILE_NAME)
                    param("newFileName", "a/b")
                }.andReturn()
                .response

        // then
        response.status shouldBe 400
        response.contentAsString shouldContain "\"details\":[\"newFileName: must not be blank or contain '/'\"]"
        verify(exactly = 0) { service.renameFile(any(), any()) }
    }

    @ParameterizedTest
    @CsvSource("$FIRST_FILE_NAME, text/plain", "zażółć.pdf, application/pdf", "archive, application/octet-stream")
    fun `should stream file with its content type and attachment name when file is requested`(
        fileName: String,
        expectedType: String,
    ) {
        // given
        val file = workDir.resolve(fileName).toFile().apply { writeText(TEST_MESSAGE) }
        every { service.getFile(fileName, null) } returns file

        // when
        val response = mockMvc.get("$FILES/$fileName").andReturn().response

        // then
        val disposition = ContentDisposition.parse(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)!!)
        assertSoftly {
            response.status shouldBe 200
            response.contentType shouldBe expectedType
            response.contentAsString shouldBe TEST_MESSAGE
            disposition.isAttachment shouldBe true
            disposition.filename shouldBe fileName
        }
    }

    @Test
    fun `should return 204 when file is deleted`() {
        // given
        every { service.deleteFile(FIRST_FILE_NAME, null) } just Runs

        // when
        val response = mockMvc.delete("$FILES/$FIRST_FILE_NAME").andReturn().response

        // then
        response.status shouldBe 204
        verify(exactly = 1) { service.deleteFile(FIRST_FILE_NAME, null) }
    }

    private companion object {
        const val FILES = "/api/v1/files"
    }
}
