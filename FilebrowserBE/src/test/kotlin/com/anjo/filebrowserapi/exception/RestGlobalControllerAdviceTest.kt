package com.anjo.filebrowserapi.exception

import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.controller.DirectoryManagerController
import com.anjo.filebrowserapi.service.DirectoryManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get

@WebMvcTest(DirectoryManagerController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("web")
@ApplyExtension(SpringExtension::class)
class RestGlobalControllerAdviceTest : FunSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var service: DirectoryManager

    init {
        context("should map domain exception to status with error body when service throws") {
            withTests(
                nameFn = { (exception, expected) -> "${exception.javaClass.simpleName} -> $expected" },
                BadInputException(TEST_MESSAGE) to 400,
                NotFoundException(TEST_MESSAGE) to 404,
                AlreadyExistsException(TEST_MESSAGE) to 409,
                NotEmptyException(TEST_MESSAGE) to 409,
                QuotaExceededException(TEST_MESSAGE) to 507,
            ) { (exception, expected) ->
                // given
                every { service.deleteDirectory(FIRST_NAME) } throws exception

                // when
                val response = deleteDirectory()

                // then
                response.status shouldBe expected
                response.contentAsString shouldBe
                    """{"error":"${exception.javaClass.simpleName}","message":"$TEST_MESSAGE","details":[]}"""
            }
        }

        test("should return 400 with error body when required parameter is missing") {
            // when
            val response = mockMvc.delete("/api/v1/directories").andReturn().response

            // then
            response.status shouldBe 400
            response.contentAsString shouldBe
                """{"error":"MissingServletRequestParameterException",""" +
                """"message":"Required parameter 'path' is not present.","details":[]}"""
        }

        test("should return 404 with error body when route is unknown") {
            // when
            val response = mockMvc.get("/api/v1/unknown").andReturn().response

            // then
            response.status shouldBe 404
            response.contentAsString shouldBe
                """{"error":"NoResourceFoundException","message":"No static resource api/v1/unknown.","details":[]}"""
        }

        test("should return 500 with generic error body when unexpected exception is thrown") {
            // given
            every { service.deleteDirectory(FIRST_NAME) } throws IllegalStateException(TEST_MESSAGE)

            // when
            val response = deleteDirectory()

            // then
            response.status shouldBe 500
            response.contentAsString shouldBe """{"error":"InternalServerError","message":"Unexpected error","details":[]}"""
        }
    }

    private fun deleteDirectory() =
        mockMvc
            .delete("/api/v1/directories") { param("path", FIRST_NAME) }
            .andReturn()
            .response
}
