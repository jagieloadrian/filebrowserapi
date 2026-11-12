package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.service.DirectoryManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import kotlin.test.Test

@WebMvcTest(DirectoryManagerController::class)
@ActiveProfiles("web")
class DirectoryManagerControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean
    private lateinit var service: DirectoryManager

    @Test
    fun `should return directories tree when GET is called`() {
        // given
        every { service.showDirectoriesTree(null) } returns listOf("/$FIRST_NAME", "/$SECOND_NAME")

        // when
        val response = mockMvc.get(DIRECTORIES).andReturn().response

        // then
        response.status shouldBe 200
        response.contentAsString shouldBe """["/$FIRST_NAME","/$SECOND_NAME"]"""
    }

    @Test
    fun `should return 201 with path when directory is created`() {
        // given
        every { service.addDirectory(FIRST_NAME, null) } returns "/$FIRST_NAME"

        // when
        val response = mockMvc.post(DIRECTORIES) { param("directoryName", FIRST_NAME) }.andReturn().response

        // then
        response.status shouldBe 201
        response.contentAsString shouldBe "/$FIRST_NAME"
    }

    @ParameterizedTest
    @ValueSource(strings = [" ", "a/b"])
    fun `should return 400 with details when directory name is blank or contains slash`(input: String) {
        // when
        val response = mockMvc.post(DIRECTORIES) { param("directoryName", input) }.andReturn().response

        // then
        response.status shouldBe 400
        response.contentAsString shouldContain "\"details\":[\"directoryName: must not be blank or contain '/'\"]"
        verify(exactly = 0) { service.addDirectory(any(), any()) }
    }

    @Test
    fun `should return 400 with details when new directory name contains slash`() {
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

    @Test
    fun `should return 201 with last path when directories are created recursively`() {
        // given
        val input = "$FIRST_NAME/$SECOND_NAME"
        every { service.addDirectoriesRecursively(input) } returns "/$input"

        // when
        val response = mockMvc.post("$DIRECTORIES/recursively") { param("path", input) }.andReturn().response

        // then
        response.status shouldBe 201
        response.contentAsString shouldBe "/$input"
    }

    @Test
    fun `should return new path when directory is renamed`() {
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

    @Test
    fun `should return 204 when directory is deleted`() {
        // given
        every { service.deleteDirectory(FIRST_NAME) } just Runs

        // when
        val response = mockMvc.delete(DIRECTORIES) { param("path", FIRST_NAME) }.andReturn().response

        // then
        response.status shouldBe 204
        verify(exactly = 1) { service.deleteDirectory(FIRST_NAME) }
    }

    private companion object {
        const val DIRECTORIES = "/api/v1/directories"
    }
}
