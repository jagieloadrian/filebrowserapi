package com.anjo.filebrowserapi.security

import com.anjo.filebrowserapi.TestTokens
import com.anjo.filebrowserapi.TestTokens.READER
import com.anjo.filebrowserapi.TestTokens.WRITER
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("web")
@ExtendWith(OutputCaptureExtension::class)
class RestSecurityTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @ParameterizedTest
    @MethodSource("allRoutes")
    fun `should return 401 with error body when route is called without token`(
        method: HttpMethod,
        uri: String,
    ) {
        // when
        val response = call(method, uri, null)

        // then
        response.status shouldBe 401
        response.getHeader(HttpHeaders.WWW_AUTHENTICATE) shouldContain "Bearer"
        response.contentAsString shouldContain "\"error\":\"InsufficientAuthenticationException\""
    }

    @ParameterizedTest
    @MethodSource("allRoutes")
    fun `should return 403 with error body when token has no filebrowser role`(
        method: HttpMethod,
        uri: String,
    ) {
        // when
        val response = call(method, uri, TestTokens.token("statistics-admin"))

        // then
        response.status shouldBe 403
        response.contentAsString shouldContain "\"error\":\"AuthorizationDeniedException\""
    }

    @ParameterizedTest
    @MethodSource("writeRoutes")
    fun `should return 403 when reader calls write route`(
        method: HttpMethod,
        uri: String,
    ) {
        // when
        val response = call(method, uri, TestTokens.token(READER))

        // then
        response.status shouldBe 403
    }

    @ParameterizedTest
    @MethodSource("readRoutes")
    fun `should pass security when reader calls read route`(
        method: HttpMethod,
        uri: String,
    ) {
        // when
        val response = call(method, uri, TestTokens.token(READER))

        // then
        listOf(401, 403) shouldNotContain response.status
    }

    @ParameterizedTest
    @MethodSource("allRoutes")
    fun `should pass security when writer calls any route`(
        method: HttpMethod,
        uri: String,
    ) {
        // when
        val response = call(method, uri, TestTokens.token(WRITER))

        // then
        listOf(401, 403) shouldNotContain response.status
    }

    @ParameterizedTest
    @MethodSource("invalidTokens")
    fun `should return 401 when token is invalid`(token: String) {
        // when
        val response = call(HttpMethod.GET, FILES, token)

        // then
        response.status shouldBe 401
        response.contentAsString shouldContain "\"error\":\"InvalidBearerTokenException\""
    }

    @ParameterizedTest
    @ValueSource(strings = ["/health/live", "/health/ready", "/v3/api-docs"])
    fun `should serve public endpoint when called without token`(uri: String) {
        // when
        val response = mockMvc.get(uri).andReturn().response

        // then
        response.status shouldBe 200
    }

    @Test
    fun `should document bearer JWT scheme for every operation when OpenAPI is requested`() {
        // when
        val response = mockMvc.get("/v3/api-docs").andReturn().response

        // then
        response.contentAsString shouldContain "\"security\":[{\"bearer\":[]}]"
        response.contentAsString shouldContain "\"bearer\":{\"type\":\"http\",\"scheme\":\"bearer\",\"bearerFormat\":\"JWT\"}"
    }

    @Test
    fun `should keep users apart when two users list directories`() {
        // given
        val owner = TestTokens.token(WRITER, subject = "alice")
        val other = TestTokens.token(WRITER, subject = "bob")
        mockMvc.post(DIRECTORIES) {
            param("directoryName", "private")
            header(HttpHeaders.AUTHORIZATION, "Bearer $owner")
        }

        // when
        val ownView = call(HttpMethod.GET, DIRECTORIES, owner).contentAsString
        val otherView = call(HttpMethod.GET, DIRECTORIES, other).contentAsString

        // then
        ownView shouldContain "/private"
        otherView shouldNotContain "private"
    }

    @Test
    fun `should never log the token when request is authorized`(output: CapturedOutput) {
        // given
        val token = TestTokens.token(WRITER)

        // when
        call(HttpMethod.GET, FILES, token)
        call(HttpMethod.DELETE, "$FILES/missing.txt", token)

        // then
        output.all shouldNotContain token
    }

    private fun call(
        method: HttpMethod,
        uri: String,
        token: String?,
    ): MockHttpServletResponse =
        mockMvc
            .request(method, uri) { token?.let { header(HttpHeaders.AUTHORIZATION, "Bearer $it") } }
            .andReturn()
            .response

    companion object {
        private const val FILES = "/api/v1/files"
        private const val DIRECTORIES = "/api/v1/directories"
        private const val ENTRIES = "/api/v1/entries"

        @JvmStatic
        fun readRoutes() =
            listOf(
                Arguments.of(HttpMethod.GET, FILES),
                Arguments.of(HttpMethod.GET, "$FILES/missing.txt"),
                Arguments.of(HttpMethod.GET, DIRECTORIES),
                Arguments.of(HttpMethod.GET, ENTRIES),
            )

        @JvmStatic
        fun writeRoutes() =
            listOf(
                Arguments.of(HttpMethod.POST, FILES),
                Arguments.of(HttpMethod.PATCH, FILES),
                Arguments.of(HttpMethod.DELETE, "$FILES/missing.txt"),
                Arguments.of(HttpMethod.POST, DIRECTORIES),
                Arguments.of(HttpMethod.POST, "$DIRECTORIES/recursively"),
                Arguments.of(HttpMethod.PATCH, DIRECTORIES),
                Arguments.of(HttpMethod.DELETE, DIRECTORIES),
                Arguments.of(HttpMethod.POST, "$ENTRIES/copy"),
                Arguments.of(HttpMethod.POST, "$ENTRIES/move"),
            )

        @JvmStatic
        fun allRoutes() = readRoutes() + writeRoutes()

        @JvmStatic
        fun invalidTokens() =
            listOf(
                TestTokens.token(WRITER, audience = "other-app"),
                TestTokens.token(WRITER, issuer = "https://evil.test"),
                TestTokens.token(WRITER, expiresAt = Instant.now().minusSeconds(3600)),
                TestTokens.token(WRITER, signingKey = TestTokens.foreignKey()),
                "not-a-jwt",
            )

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("application.path") { Files.createTempDirectory("filebrowser-security").toString() }
            TestTokens.register(registry)
        }
    }
}
