package com.anjo.filebrowserapi.security

import com.anjo.filebrowserapi.TestTokens
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.options
import java.nio.file.Files

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("web")
@ApplyExtension(SpringExtension::class)
class CorsTest : FunSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    init {
        context("should answer preflight without token when origin is allowed") {
            withTests("/api/v1/files", "/share/x") { uri ->
                // when
                val response = mockMvc.preflight(uri, ALLOWED).andReturn().response

                // then
                response.status shouldBe 200
                response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) shouldBe ALLOWED
                response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS) shouldBe "GET,POST,PUT,PATCH,DELETE,OPTIONS"
                response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS) shouldBe "Authorization, Content-Type"
            }
        }

        test("should add no CORS headers when origin is not allowed") {
            // when
            val response = mockMvc.preflight("/api/v1/files", "http://evil.test").andReturn().response

            // then
            response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN).shouldBeNull()
        }

        test("should expose Content-Disposition when allowed origin calls API") {
            // when
            val response = mockMvc.get("/share/unknown") { header(HttpHeaders.ORIGIN, ALLOWED) }.andReturn().response

            // then
            response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) shouldBe ALLOWED
            response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS) shouldContain HttpHeaders.CONTENT_DISPOSITION
        }
    }

    companion object {
        private const val ALLOWED = "http://localhost:8081"

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("application.path") { Files.createTempDirectory("filebrowser-cors").toString() }
            registry.add("application.cors.allowed-origins") { ALLOWED }
            TestTokens.register(registry)
        }
    }
}

internal fun MockMvc.preflight(
    uri: String,
    origin: String,
) = options(uri) {
    header(HttpHeaders.ORIGIN, origin)
    header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
    header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type")
}
