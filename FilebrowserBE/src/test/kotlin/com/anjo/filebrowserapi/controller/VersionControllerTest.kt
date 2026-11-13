package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.model.FileVersion
import com.anjo.filebrowserapi.service.VersionManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.UUID
import kotlin.test.Test

@WebMvcTest(VersionController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("web")
class VersionControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean
    private lateinit var service: VersionManager

    private val id = UUID.fromString("00000000-0000-0000-0000-000000000002")

    @Test
    fun `should list versions and restore one`() {
        // given
        every { service.list("a.txt") } returns listOf(FileVersion(id, "/a.txt", 3, Instant.parse("2026-11-13T10:00:00Z")))
        every { service.restore("$id") } returns "/a.txt"

        // when
        val listed = mockMvc.get(VERSIONS) { param("path", "a.txt") }.andReturn().response
        val restored = mockMvc.post("$VERSIONS/$id/restore").andReturn().response

        // then
        listed.contentAsString shouldBe """[{"id":"$id","path":"/a.txt","size":3,"createdAt":"2026-11-13T10:00:00Z"}]"""
        restored.status shouldBe 200
        restored.contentAsString shouldBe "/a.txt"
    }

    private companion object {
        const val VERSIONS = "/api/v1/versions"
    }
}
