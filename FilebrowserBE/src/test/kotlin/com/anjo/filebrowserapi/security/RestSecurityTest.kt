package com.anjo.filebrowserapi.security

import com.anjo.filebrowserapi.TestObjects.captureOutput
import com.anjo.filebrowserapi.TestTokens
import com.anjo.filebrowserapi.TestTokens.READER
import com.anjo.filebrowserapi.TestTokens.WRITER
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request
import java.nio.file.Files
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("web")
@ApplyExtension(SpringExtension::class)
class RestSecurityTest : FunSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    init {
        context("should return 401 with error body when route is called without token") {
            withTests(nameFn = routeName, allRoutes()) { (method, uri) ->
                // when
                val response = call(method, uri, null)

                // then
                response.status shouldBe 401
                response.getHeader(HttpHeaders.WWW_AUTHENTICATE) shouldContain "Bearer"
                response.contentAsString shouldContain "\"error\":\"InsufficientAuthenticationException\""
            }
        }

        context("should return 403 with error body when token has no filebrowser role") {
            withTests(nameFn = routeName, allRoutes()) { (method, uri) ->
                // when
                val response = call(method, uri, TestTokens.token("statistics-admin"))

                // then
                response.status shouldBe 403
                response.contentAsString shouldContain "\"error\":\"AuthorizationDeniedException\""
            }
        }

        context("should return 403 when reader calls write route") {
            withTests(nameFn = routeName, writeRoutes()) { (method, uri) ->
                // when
                val response = call(method, uri, TestTokens.token(READER))

                // then
                response.status shouldBe 403
            }
        }

        context("should pass security when reader calls read route") {
            withTests(nameFn = routeName, readRoutes()) { (method, uri) ->
                // when
                val response = call(method, uri, TestTokens.token(READER))

                // then
                listOf(401, 403) shouldNotContain response.status
            }
        }

        context("should pass security when writer calls any route") {
            withTests(nameFn = routeName, allRoutes()) { (method, uri) ->
                // when
                val response = call(method, uri, TestTokens.token(WRITER))

                // then
                listOf(401, 403) shouldNotContain response.status
            }
        }

        context("should return 401 when token is invalid") {
            withTests(invalidTokens()) { token ->
                // when
                val response = call(HttpMethod.GET, FILES, token)

                // then
                response.status shouldBe 401
                response.contentAsString shouldContain "\"error\":\"InvalidBearerTokenException\""
            }
        }

        context("should serve public endpoint when called without token") {
            withTests("/health/live", "/health/ready", "/v3/api-docs") { uri ->
                // when
                val response = mockMvc.get(uri).andReturn().response

                // then
                response.status shouldBe 200
            }
        }

        test("should document bearer JWT scheme for every operation when OpenAPI is requested") {
            // when
            val response = mockMvc.get("/v3/api-docs").andReturn().response

            // then
            response.contentAsString shouldContain "\"security\":[{\"bearer\":[]}]"
            response.contentAsString shouldContain "\"bearer\":{\"type\":\"http\",\"scheme\":\"bearer\",\"bearerFormat\":\"JWT\"}"
        }

        test("should keep users apart when two users list directories") {
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

        test("should never log the token when request is authorized") {
            // given
            val token = TestTokens.token(WRITER)

            // when
            val output =
                captureOutput {
                    call(HttpMethod.GET, FILES, token)
                    call(HttpMethod.DELETE, "$FILES/missing.txt", token)
                }

            // then
            output shouldNotContain token
        }

        test("should serve shared file without token and never log the share token") {
            lateinit var created: MockHttpServletResponse
            lateinit var shareToken: String
            lateinit var shared: MockHttpServletResponse
            lateinit var unknown: MockHttpServletResponse
            val output =
                captureOutput {
                    // given
                    val owner = TestTokens.token(WRITER, subject = "carol")
                    mockMvc.multipart(FILES) {
                        file(MockMultipartFile("uploadFile", "shared.txt", "text/plain", "shared".toByteArray()))
                        header(HttpHeaders.AUTHORIZATION, "Bearer $owner")
                    }
                    created =
                        mockMvc
                            .post(SHARES) {
                                param("path", "shared.txt")
                                header(HttpHeaders.AUTHORIZATION, "Bearer $owner")
                            }.andReturn()
                            .response
                    shareToken = Regex("\"token\":\"([^\"]+)\"").find(created.contentAsString)!!.groupValues[1]

                    // when
                    shared = call(HttpMethod.GET, "/share/$shareToken", null)
                    unknown = call(HttpMethod.GET, "/share/unknown", null)
                }

            // then
            created.status shouldBe 201
            shared.status shouldBe 200
            shared.contentAsString shouldBe "shared"
            unknown.status shouldBe 404
            output shouldNotContain shareToken
        }
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
        private const val TRASH = "/api/v1/trash"
        private const val SHARES = "/api/v1/shares"

        private val routeName: (Pair<HttpMethod, String>) -> String = { (method, uri) -> "$method $uri" }

        private fun readRoutes() =
            listOf(
                HttpMethod.GET to FILES,
                HttpMethod.GET to "$FILES/missing.txt",
                HttpMethod.GET to DIRECTORIES,
                HttpMethod.GET to ENTRIES,
                HttpMethod.GET to "$ENTRIES/duplicates",
                HttpMethod.GET to TRASH,
                HttpMethod.GET to "/api/v1/versions",
                HttpMethod.GET to SHARES,
                HttpMethod.GET to "/api/v1/quota",
            )

        private fun writeRoutes() =
            listOf(
                HttpMethod.POST to FILES,
                HttpMethod.PATCH to FILES,
                HttpMethod.DELETE to "$FILES/missing.txt",
                HttpMethod.POST to DIRECTORIES,
                HttpMethod.POST to "$DIRECTORIES/recursively",
                HttpMethod.PATCH to DIRECTORIES,
                HttpMethod.DELETE to DIRECTORIES,
                HttpMethod.DELETE to "$DIRECTORIES/recursively",
                HttpMethod.PUT to "$ENTRIES/meta",
                HttpMethod.POST to "$TRASH/x/restore",
                HttpMethod.DELETE to "$TRASH/x",
                HttpMethod.DELETE to TRASH,
                HttpMethod.POST to "/api/v1/versions/x/restore",
                HttpMethod.POST to SHARES,
                HttpMethod.DELETE to "$SHARES/x",
                HttpMethod.POST to "$ENTRIES/copy",
                HttpMethod.POST to "$ENTRIES/move",
            )

        private fun allRoutes() = readRoutes() + writeRoutes()

        private fun invalidTokens() =
            mapOf(
                "wrong audience" to TestTokens.token(WRITER, audience = "other-app"),
                "wrong issuer" to TestTokens.token(WRITER, issuer = "https://evil.test"),
                "expired" to TestTokens.token(WRITER, expiresAt = Instant.now().minusSeconds(3600)),
                "foreign signing key" to TestTokens.token(WRITER, signingKey = TestTokens.foreignKey()),
                "not a JWT" to "not-a-jwt",
            )

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("application.path") { Files.createTempDirectory("filebrowser-security").toString() }
            TestTokens.register(registry)
        }
    }
}
