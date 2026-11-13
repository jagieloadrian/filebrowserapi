package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.model.Entry
import com.anjo.filebrowserapi.service.EntryManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import kotlin.test.Test

@WebMvcTest(EntryController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("web")
class EntryControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean
    private lateinit var service: EntryManager

    @Test
    fun `should return entries with metadata when GET is called with filters`() {
        // given
        val entry = Entry("/$FIRST_NAME/$FIRST_FILE_NAME", false, 4, Instant.parse("2026-11-13T10:00:00Z"), "text/plain")
        every { service.listEntries(FIRST_NAME, "first", "txt") } returns listOf(entry)

        // when
        val response =
            mockMvc
                .get(ENTRIES) {
                    param("path", FIRST_NAME)
                    param("name", "first")
                    param("extension", "txt")
                }.andReturn()
                .response

        // then
        response.status shouldBe 200
        response.contentAsString shouldBe
            """[{"path":"/$FIRST_NAME/$FIRST_FILE_NAME","directory":false,"size":4,"modified":"2026-11-13T10:00:00Z","contentType":"text/plain"}]"""
    }

    @ParameterizedTest
    @CsvSource("copy,201", "move,200")
    fun `should return target path when entry is copied or moved`(
        operation: String,
        status: Int,
    ) {
        // given
        every { service.copyEntry(FIRST_NAME, "/copy") } returns "/copy"
        every { service.moveEntry(FIRST_NAME, "/copy") } returns "/copy"

        // when
        val response =
            mockMvc
                .post("$ENTRIES/$operation") {
                    param("source", FIRST_NAME)
                    param("target", "/copy")
                }.andReturn()
                .response

        // then
        response.status shouldBe status
        response.contentAsString shouldBe "/copy"
    }

    private companion object {
        const val ENTRIES = "/api/v1/entries"
    }
}
