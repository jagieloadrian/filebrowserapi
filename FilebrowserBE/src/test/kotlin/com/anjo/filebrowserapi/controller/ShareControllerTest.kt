package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.model.CreatedShare
import com.anjo.filebrowserapi.model.Share
import com.anjo.filebrowserapi.service.ShareManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.test.Test

@WebMvcTest(ShareController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("web")
class ShareControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean
    private lateinit var service: ShareManager

    @TempDir
    private lateinit var workDir: Path

    private val share =
        Share(
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            "/a.txt",
            Instant.parse("2026-11-13T10:00:00Z"),
            Instant.parse("2026-11-20T10:00:00Z"),
        )

    @Test
    fun `should create list and revoke shares`() {
        // given
        every { service.create("a.txt", 7) } returns CreatedShare(share, "tok")
        every { service.list() } returns listOf(share)
        every { service.revoke("${share.id}") } just Runs
        val json = """{"id":"${share.id}","path":"/a.txt","createdAt":"2026-11-13T10:00:00Z","expiresAt":"2026-11-20T10:00:00Z"}"""

        // when
        val created =
            mockMvc
                .post(SHARES) {
                    param("path", "a.txt")
                    param("days", "7")
                }.andReturn()
                .response
        val listed = mockMvc.get(SHARES).andReturn().response
        val revoked = mockMvc.delete("$SHARES/${share.id}").andReturn().response

        // then
        created.status shouldBe 201
        created.contentAsString shouldBe """{"share":$json,"token":"tok"}"""
        listed.contentAsString shouldBe "[$json]"
        revoked.status shouldBe 204
    }

    @Test
    fun `should download shared file as attachment`() {
        // given
        val file = File(workDir.toFile(), "a.txt").apply { writeText("shared") }
        every { service.open("tok") } returns file

        // when
        val response = mockMvc.get("/share/tok").andReturn().response

        // then
        response.status shouldBe 200
        response.contentAsString shouldBe "shared"
        response.getHeader(HttpHeaders.CONTENT_DISPOSITION)!! shouldContain "attachment"
    }

    private companion object {
        const val SHARES = "/api/v1/shares"
    }
}
