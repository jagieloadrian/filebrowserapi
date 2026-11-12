package com.anjo.filebrowserapi.security

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("web", "local")
class LocalProfileTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `should serve API without token when local profile is active`() {
        // given
        mockMvc.post(DIRECTORIES) { param("directoryName", "notes") }

        // when
        val response = mockMvc.get(DIRECTORIES).andReturn().response

        // then
        response.status shouldBe 200
        response.contentAsString shouldContain "/notes"
        root.resolve("filebrowser/local/notes").isDirectory() shouldBe true
    }

    companion object {
        private const val DIRECTORIES = "/api/v1/directories"
        private val root: Path = Files.createTempDirectory("filebrowser-local")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("application.path") { root.toString() }
        }
    }
}
