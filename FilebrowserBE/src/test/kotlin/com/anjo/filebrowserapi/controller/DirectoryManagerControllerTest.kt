package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.service.DirectoryManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@WebMvcTest(DirectoryManagerController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("web")
@ApplyExtension(SpringExtension::class)
class DirectoryManagerControllerTest : FunSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var service: DirectoryManager

    init {
        test("should return directories tree when GET is called") {
            // given
            every { service.showDirectoriesTree(null) } returns listOf("/$FIRST_NAME", "/$SECOND_NAME")

            // when
            val response = mockMvc.get(DIRECTORIES).andReturn().response

            // then
            response.status shouldBe 200
            response.contentAsString shouldBe """["/$FIRST_NAME","/$SECOND_NAME"]"""
        }

        test("should return 201 with path when directory is created") {
            // given
            every { service.addDirectory(FIRST_NAME, null) } returns "/$FIRST_NAME"

            // when
            val response = mockMvc.post(DIRECTORIES) { param("directoryName", FIRST_NAME) }.andReturn().response

            // then
            response.status shouldBe 201
            response.contentAsString shouldBe "/$FIRST_NAME"
        }

        context("should return 400 with details when directory name is blank or contains slash") {
            withTests(nameFn = { "'$it'" }, " ", "a/b") { input ->
                // when
                val response = mockMvc.post(DIRECTORIES) { param("directoryName", input) }.andReturn().response

                // then
                response.status shouldBe 400
                response.contentAsString shouldContain "\"details\":[\"directoryName: must not be blank or contain '/'\"]"
                verify(exactly = 0) { service.addDirectory(any(), any()) }
            }
        }

        test("should return 400 with details when new directory name contains slash") {
            // when
            val response =
                mockMvc
                    .patch(DIRECTORIES) {
                        param("sourcePath", FIRST_NAME)
                        param("newDirName", "a/b")
                    }.andReturn()
                    .response

            // then
            response.status shouldBe 400
            response.contentAsString shouldContain "\"details\":[\"newDirName: must not be blank or contain '/'\"]"
            verify(exactly = 0) { service.renameDirectory(any(), any()) }
        }

        test("should return 201 with last path when directories are created recursively") {
            // given
            val input = "$FIRST_NAME/$SECOND_NAME"
            every { service.addDirectoriesRecursively(input) } returns "/$input"

            // when
            val response = mockMvc.post("$DIRECTORIES/recursively") { param("path", input) }.andReturn().response

            // then
            response.status shouldBe 201
            response.contentAsString shouldBe "/$input"
        }

        test("should return new path when directory is renamed") {
            // given
            every { service.renameDirectory(SECOND_NAME, FIRST_NAME) } returns "/$SECOND_NAME"

            // when
            val response =
                mockMvc
                    .patch(DIRECTORIES) {
                        param("sourcePath", FIRST_NAME)
                        param("newDirName", SECOND_NAME)
                    }.andReturn()
                    .response

            // then
            response.status shouldBe 200
            response.contentAsString shouldBe "/$SECOND_NAME"
        }

        test("should return 204 when directory is deleted") {
            // given
            every { service.deleteDirectory(FIRST_NAME) } just Runs

            // when
            val response = mockMvc.delete(DIRECTORIES) { param("path", FIRST_NAME) }.andReturn().response

            // then
            response.status shouldBe 204
            verify(exactly = 1) { service.deleteDirectory(FIRST_NAME) }
        }

        test("should return 204 when directory is deleted recursively") {
            // given
            every { service.deleteDirectoryRecursively(FIRST_NAME) } just Runs

            // when
            val response = mockMvc.delete("$DIRECTORIES/recursively") { param("path", FIRST_NAME) }.andReturn().response

            // then
            response.status shouldBe 204
            verify(exactly = 1) { service.deleteDirectoryRecursively(FIRST_NAME) }
        }
    }

    private companion object {
        const val DIRECTORIES = "/api/v1/directories"
    }
}
