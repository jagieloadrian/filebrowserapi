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
        response.contentAsString shouldBe """{"error":"CannotRealizeException","message":"$TEST_MESSAGE"}"""
    }

    @Test
    fun `should return 400 with error body when BadInputException is thrown`() {
        // given
        every { service.deleteDirectory(FIRST_NAME) } throws BadInputException(TEST_MESSAGE)

        // when
        val response = deleteDirectory()

        // then
        response.status shouldBe 400
        response.contentAsString shouldBe """{"error":"BadInputException","message":"$TEST_MESSAGE"}"""
    }

    private fun deleteDirectory() =
        mockMvc
            .delete("/api/v1/directories") { param("path", FIRST_NAME) }
            .andReturn()
            .response
}
