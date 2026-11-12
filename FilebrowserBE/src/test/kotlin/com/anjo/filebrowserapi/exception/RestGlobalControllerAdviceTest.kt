package com.anjo.filebrowserapi.exception

import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.controller.DirectoryManagerController
import com.anjo.filebrowserapi.service.DirectoryManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import kotlin.test.Test

@WebMvcTest(DirectoryManagerController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("web")
class RestGlobalControllerAdviceTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean
    private lateinit var service: DirectoryManager

    @ParameterizedTest
    @MethodSource("domainErrors")
    fun `should map domain exception to status with error body when service throws`(
        exception: FileBrowserException,
        expected: Int,
    ) {
        // given
        every { service.deleteDirectory(FIRST_NAME) } throws exception

        // when
        val response = deleteDirectory()

        // then
        response.status shouldBe expected
        response.contentAsString shouldBe
            """{"error":"${exception.javaClass.simpleName}","message":"$TEST_MESSAGE","details":[]}"""
    }

    @Test
    fun `should return 400 with error body when required parameter is missing`() {
        // when
        val response = mockMvc.delete("/api/v1/directories").andReturn().response

        // then
        response.status shouldBe 400
        response.contentAsString shouldBe
            """{"error":"MissingServletRequestParameterException",""" +
            """"message":"Required parameter 'path' is not present.","details":[]}"""
    }

    @Test
    fun `should return 404 with error body when route is unknown`() {
        // when
        val response = mockMvc.get("/api/v1/unknown").andReturn().response

        // then
        response.status shouldBe 404
        response.contentAsString shouldBe
            """{"error":"NoResourceFoundException","message":"No static resource api/v1/unknown.","details":[]}"""
    }

    @Test
    fun `should return 500 with generic error body when unexpected exception is thrown`() {
        // given
        every { service.deleteDirectory(FIRST_NAME) } throws IllegalStateException(TEST_MESSAGE)

        // when
        val response = deleteDirectory()

        // then
        response.status shouldBe 500
        response.contentAsString shouldBe """{"error":"InternalServerError","message":"Unexpected error","details":[]}"""
    }

    private fun deleteDirectory() =
        mockMvc
            .delete("/api/v1/directories") { param("path", FIRST_NAME) }
            .andReturn()
            .response

    companion object {
        @JvmStatic
        fun domainErrors() =
            listOf(
                Arguments.of(BadInputException(TEST_MESSAGE), 400),
                Arguments.of(NotFoundException(TEST_MESSAGE), 404),
                Arguments.of(AlreadyExistsException(TEST_MESSAGE), 409),
                Arguments.of(NotEmptyException(TEST_MESSAGE), 409),
            )
    }
}
