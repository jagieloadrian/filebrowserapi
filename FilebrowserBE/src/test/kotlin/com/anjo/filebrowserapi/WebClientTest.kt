package com.anjo.filebrowserapi

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotStartWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.client.RestTestClient
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

private const val INDEX = "<!DOCTYPE html><title>Filebrowser</title>"
private val WASM = byteArrayOf(0, 0x61, 0x73, 0x6D, 1, 0, 0, 0)

private fun RestTestClient.page(
    uri: String,
    etag: String? = null,
) = get()
    .uri(uri)
    .accept(MediaType.TEXT_HTML)
    .headers { headers -> etag?.let { headers.setIfNoneMatch(it) } }
    .exchange()
    .returnResult(String::class.java)

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("web")
@ApplyExtension(SpringExtension::class)
class WebClientTest : FunSpec() {
    @Autowired
    private lateinit var client: RestTestClient

    init {
        context("should serve index.html without token and revalidate it when the web client is opened") {
            withTests("/", "/?code=c&state=s", "/index.html") { uri ->
                // when
                val result = client.page(uri)

                // then
                result.status.value() shouldBe 200
                result.responseBody shouldBe INDEX
                result.responseHeaders.contentType?.isCompatibleWith(MediaType.TEXT_HTML) shouldBe true
                result.responseHeaders.cacheControl shouldBe CacheControl.noCache().headerValue
                result.responseHeaders.getFirst(HttpHeaders.LAST_MODIFIED).shouldBeNull()
            }
        }

        test("should serve wasm as application/wasm when the browser loads the module") {
            // when
            val result =
                client
                    .get()
                    .uri("/app.wasm")
                    .exchange()
                    .returnResult(ByteArray::class.java)

            // then
            result.status.value() shouldBe 200
            result.responseHeaders.contentType shouldBe MediaType.parseMediaType("application/wasm")
            result.responseBody shouldBe WASM
        }

        context("should answer 304 without body when If-None-Match carries the content ETag") {
            withTests("/", "/index.html", "/app.wasm") { uri ->
                // given
                val etag = client.page(uri).responseHeaders.eTag

                // when
                val result = client.page(uri, etag)

                // then
                etag.shouldNotBeNull().shouldNotStartWith("W/")
                result.status.value() shouldBe 304
                result.responseBody.shouldBeNull()
                result.responseHeaders.eTag shouldBe etag
                result.responseHeaders.cacheControl shouldBe CacheControl.noCache().headerValue
            }
        }

        test("should send the same ETag for the same file and a different one when content differs") {
            // when
            val welcome = client.page("/").responseHeaders.eTag
            val index = client.page("/index.html").responseHeaders.eTag
            val wasm = client.page("/app.wasm").responseHeaders.eTag

            // then
            index.shouldNotBeNull() shouldBe welcome
            wasm.shouldNotBeNull() shouldNotBe index
        }

        test("should return 404 when a missing web client file is requested") {
            // when
            val result = client.page("/missing.js")

            // then
            result.status.value() shouldBe 404
        }

        context("should return 401 when API or non-GET request is sent without token") {
            withTests(
                nameFn = { "${it.first} ${it.second}" },
                HttpMethod.GET to "/api/v1/quota",
                HttpMethod.GET to "/api/other",
                HttpMethod.POST to "/index.html",
            ) { (method, uri) ->
                // when
                val result =
                    client
                        .method(method)
                        .uri(uri)
                        .exchange()
                        .returnResult(String::class.java)

                // then
                result.status.value() shouldBe 401
            }
        }
    }

    companion object {
        private val ui: Path =
            Files.createTempDirectory("filebrowser-ui").also {
                it.resolve("index.html").writeText(INDEX)
                it.resolve("app.wasm").writeBytes(WASM)
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("application.path") { Files.createTempDirectory("filebrowser-web-client").toString() }
            registry.add("spring.web.resources.static-locations") { ui.toUri().toString() }
            TestTokens.register(registry)
        }
    }
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("web")
@ApplyExtension(SpringExtension::class)
class WebClientDisabledTest : FunSpec() {
    @Autowired
    private lateinit var client: RestTestClient

    init {
        test("should not serve a web client when no static location is configured") {
            // when
            val result = client.page("/")

            // then
            result.status.value() shouldBe 404
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("application.path") { Files.createTempDirectory("filebrowser-no-web-client").toString() }
            TestTokens.register(registry)
        }
    }
}
