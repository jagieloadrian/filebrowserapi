package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.model.TrashItem
import com.anjo.filebrowserapi.service.TrashManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.UUID

@WebMvcTest(TrashController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("web")
@ApplyExtension(SpringExtension::class)
class TrashControllerTest : FunSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var service: TrashManager

    private val id = UUID.fromString("00000000-0000-0000-0000-000000000001")

    init {
        test("should list trash items when GET is called") {
            // given
            every { service.list() } returns listOf(TrashItem(id, "/a.txt", false, 3, Instant.parse("2026-11-13T10:00:00Z")))

            // when
            val response = mockMvc.get(TRASH).andReturn().response

            // then
            response.status shouldBe 200
            response.contentAsString shouldBe
                """[{"id":"$id","path":"/a.txt","directory":false,"size":3,"deletedAt":"2026-11-13T10:00:00Z"}]"""
        }

        test("should restore, purge and empty trash") {
            // given
            every { service.restore("$id") } returns "/a.txt"
            every { service.purge("$id") } just runs
            every { service.empty() } just runs

            // when
            val restored = mockMvc.post("$TRASH/$id/restore").andReturn().response
            val purged = mockMvc.delete("$TRASH/$id").andReturn().response
            val emptied = mockMvc.delete(TRASH).andReturn().response

            // then
            restored.status shouldBe 200
            restored.contentAsString shouldBe "/a.txt"
            purged.status shouldBe 204
            emptied.status shouldBe 204
            verify(exactly = 1) { service.empty() }
        }
    }

    private companion object {
        const val TRASH = "/api/v1/trash"
    }
}
