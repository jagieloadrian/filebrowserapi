package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.service.FileManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import kotlin.test.Test

@WebMvcTest(FileManagerController::class)
@ActiveProfiles("web")
class FileManagerControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean
    private lateinit var service: FileManager

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
        every { service.uploadFile(any(), FIRST_NAME) } returns "/$FIRST_NAME/$FIRST_FILE_NAME"

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
        every { service.uploadFile(any(), null, true) } returns "/$FIRST_FILE_NAME"

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
        verify(exactly = 1) { service.uploadFile(any(), null, true) }
    }

    @Test
    fun `should return file bytes when file is requested`() {
        // given
        every { service.getFile(FIRST_FILE_NAME, null) } returns TEST_MESSAGE.toByteArray()

        // when
        val response = mockMvc.get("$FILES/$FIRST_FILE_NAME").andReturn().response

        // then
        response.status shouldBe 200
        response.contentAsByteArray shouldBe TEST_MESSAGE.toByteArray()
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
