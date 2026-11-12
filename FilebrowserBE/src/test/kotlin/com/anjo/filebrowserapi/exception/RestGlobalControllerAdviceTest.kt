package com.anjo.filebrowserapi.exception

import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.controller.DirectoryManagerController
import com.anjo.filebrowserapi.service.DirectoryManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import kotlin.test.Test

@WebMvcTest(DirectoryManagerController::class)
@ActiveProfiles("web")
class RestGlobalControllerAdviceTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean
    private lateinit var service: DirectoryManager

    @Test
    fun `should return 422 with error body when CannotRealizeException is thrown`() {
        // given
        every { service.deleteDirectory(FIRST_NAME) } throws CannotRealizeException(TEST_MESSAGE)

        // when
        val response = deleteDirectory()

        // then
        response.status shouldBe 422
        response.contentAsString shouldBe """{"error":"CannotRealizeException","message":"$TEST_MESSAGE","details":[]}"""
    }

    @Test
    fun `should return 400 with error body when BadInputException is thrown`() {
        // given
        every { service.deleteDirectory(FIRST_NAME) } throws BadInputException(TEST_MESSAGE)

        // when
        val response = deleteDirectory()

        // then
        response.status shouldBe 400
        response.contentAsString shouldBe """{"error":"BadInputException","message":"$TEST_MESSAGE","details":[]}"""
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
}
