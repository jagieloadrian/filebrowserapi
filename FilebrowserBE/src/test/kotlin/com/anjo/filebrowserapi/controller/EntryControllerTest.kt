package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.model.DuplicateGroup
import com.anjo.filebrowserapi.model.Entry
import com.anjo.filebrowserapi.service.EntryManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Instant

@WebMvcTest(EntryController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("web")
@ApplyExtension(SpringExtension::class)
class EntryControllerTest : FunSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var service: EntryManager

    init {
        test("should return entries with metadata when GET is called with filters") {
            // given
            val entry = Entry("/$FIRST_NAME/$FIRST_FILE_NAME", false, 4, Instant.parse("2026-11-13T10:00:00Z"), "text/plain")
            every { service.listEntries(FIRST_NAME, "first", "txt", null) } returns listOf(entry)

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
                """[{"path":"/$FIRST_NAME/$FIRST_FILE_NAME","directory":false,"size":4,"modified":"2026-11-13T10:00:00Z","contentType":"text/plain","description":null,"tags":[]}]"""
        }

        context("should return target path when entry is copied or moved") {
            withTests("copy" to 201, "move" to 200) { (operation, status) ->
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
        }

        test("should return duplicate groups when duplicates are requested") {
            // given
            every { service.findDuplicates(null) } returns listOf(DuplicateGroup("ab", 4, listOf("/a.txt", "/b.txt")))

            // when
            val response = mockMvc.get("$ENTRIES/duplicates").andReturn().response

            // then
            response.status shouldBe 200
            response.contentAsString shouldBe """[{"sha256":"ab","size":4,"paths":["/a.txt","/b.txt"]}]"""
        }

        test("should return updated entry when metadata is saved") {
            // given
            val entry = Entry("/$FIRST_NAME", true, 0, Instant.parse("2026-11-13T10:00:00Z"), null, "notes", listOf("work"))
            every { service.updateMeta(FIRST_NAME, "notes", listOf("work")) } returns entry

            // when
            val response =
                mockMvc
                    .put("$ENTRIES/meta") {
                        param("path", FIRST_NAME)
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"description":"notes","tags":["work"]}"""
                    }.andReturn()
                    .response

            // then
            response.status shouldBe 200
            response.contentAsString shouldContain """"tags":["work"]"""
        }

        test("should return 400 with details when a tag breaks the rule") {
            // when
            val response =
                mockMvc
                    .put("$ENTRIES/meta") {
                        param("path", FIRST_NAME)
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"tags":["a,b"]}"""
                    }.andReturn()
                    .response

            // then
            response.status shouldBe 400
            response.contentAsString shouldContain "tags: tags must be"
        }
    }

    private companion object {
        const val ENTRIES = "/api/v1/entries"
    }
}
