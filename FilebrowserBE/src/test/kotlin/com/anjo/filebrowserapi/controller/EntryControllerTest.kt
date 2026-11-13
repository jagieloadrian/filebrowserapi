package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.model.Entry
import com.anjo.filebrowserapi.service.EntryManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
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

    private companion object {
        const val ENTRIES = "/api/v1/entries"
    }
}
