package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.ui.file
import com.anjo.filebrowser.ui.folder
import com.anjo.filebrowser.ui.model.PickedFile
import com.anjo.filebrowser.ui.model.Quota
import com.anjo.filebrowser.ui.trashed
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.io.IOException
import kotlin.time.Instant

private const val BASE = "http://localhost:8080"

private class Server(
    body: String = "",
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    val requests = mutableListOf<HttpRequestData>()
    val api =
        HttpFilebrowserApi(
            HttpClient(
                MockEngine { request ->
                    requests += request
                    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                },
            ),
            BASE,
        )
    val request get() = requests.single()
}

private fun HttpRequestData.call() = "$method ${url.encodedPath}?${url.encodedQuery}"

private data class StatusCase(
    val status: HttpStatusCode,
    val expected: ApiStatus,
)

class HttpFilebrowserApiTest :
    FunSpec({
        context("reading") {
            test("should list entries below the path filtered by name and map the JSON") {
                // given
                val server =
                    Server(
                        """[{"path":"/Docs/report.txt","directory":false,"size":4,"modified":"2026-11-13T10:00:00Z",
                            "contentType":"text/plain","description":null,"tags":[]},
                            {"path":"/Docs/old","directory":true,"size":0,"modified":"2026-11-13T10:00:00.5Z","contentType":null}]""",
                    )

                // when
                val entries = server.api.listEntries("/Docs", "rep")

                // then
                server.request.call() shouldBe "${HttpMethod.Get} /api/v1/entries?path=%2FDocs&name=rep"
                entries shouldBe
                    listOf(
                        file("/Docs/report.txt", size = 4),
                        folder("/Docs/old").copy(modified = Instant.parse("2026-11-13T10:00:00.5Z")),
                    )
            }

            test("should list entries without a name filter") {
                // given
                val server = Server("[]")

                // when
                server.api.listEntries("", null)

                // then
                server.request.url.parameters
                    .names() shouldBe setOf("path")
            }

            test("should download a file by name from its folder") {
                // given
                val server = Server("hello")

                // when
                val bytes = server.api.download(file("/Docs/my report.txt"))

                // then
                server.request.call() shouldBe "${HttpMethod.Get} /api/v1/files/my%20report.txt?path=%2FDocs"
                bytes.decodeToString() shouldBe "hello"
            }

            test("should map the trash and the quota") {
                // given
                val trash = Server("""[{"id":"7","path":"/a.txt","directory":false,"size":1,"deletedAt":"2026-11-13T10:00:00Z"}]""")
                val quota = Server("""{"used":5,"limit":10}""")

                // when & then
                trash.api.trash() shouldBe listOf(trashed("7", "/a.txt"))
                trash.request.call() shouldBe "${HttpMethod.Get} /api/v1/trash?"
                quota.api.quota() shouldBe Quota(used = 5, limit = 10)
                quota.request.call() shouldBe "${HttpMethod.Get} /api/v1/quota?"
            }
        }

        context("writing") {
            test("should upload the file as the multipart part uploadFile") {
                // given
                val server = Server()

                // when
                server.api.upload("/Docs", PickedFile("a \"b\".txt", "data".encodeToByteArray()), overwrite = true)

                // then
                server.request.call() shouldBe "${HttpMethod.Post} /api/v1/files?path=%2FDocs&overwrite=true"
                val body =
                    server.request.body
                        .toByteArray()
                        .decodeToString()
                body shouldContain """form-data; name="uploadFile"; filename="a \"b\".txt""""
                body shouldContain "data"
            }

            test("should create a folder in the parent") {
                // given
                val server = Server()

                // when
                server.api.createFolder("/Docs", "Notes")

                // then
                server.request.call() shouldBe "${HttpMethod.Post} /api/v1/directories?directoryName=Notes&path=%2FDocs"
            }

            test("should rename files and folders through their own endpoints") {
                // given
                val server = Server()

                // when
                server.api.rename(file("/Docs/a.txt"), "b.txt")
                server.api.rename(folder("/Docs"), "Papers")

                // then
                server.requests.map { it.call() } shouldBe
                    listOf(
                        "${HttpMethod.Patch} /api/v1/files?sourcePath=%2FDocs%2Fa.txt&newFileName=b.txt",
                        "${HttpMethod.Patch} /api/v1/directories?sourcePath=%2FDocs&newDirName=Papers",
                    )
            }

            test("should move files and whole folders to the trash") {
                // given
                val server = Server()

                // when
                server.api.delete(file("/Docs/a.txt"))
                server.api.delete(folder("/Docs"))

                // then
                server.requests.map { it.call() } shouldBe
                    listOf(
                        "${HttpMethod.Delete} /api/v1/files/a.txt?path=%2FDocs",
                        "${HttpMethod.Delete} /api/v1/directories/recursively?path=%2FDocs",
                    )
            }

            test("should restore, purge and empty the trash") {
                // given
                val server = Server()
                val item = trashed("7", "/a.txt")

                // when
                server.api.restore(item)
                server.api.deleteForever(item)
                server.api.emptyTrash()

                // then
                server.requests.map { it.call() } shouldBe
                    listOf(
                        "${HttpMethod.Post} /api/v1/trash/7/restore?",
                        "${HttpMethod.Delete} /api/v1/trash/7?",
                        "${HttpMethod.Delete} /api/v1/trash?",
                    )
            }
        }

        context("errors") {
            withTests(
                nameFn = { "should map ${it.status.value} to ${it.expected}" },
                StatusCase(HttpStatusCode.BadRequest, ApiStatus.INVALID),
                StatusCase(HttpStatusCode.Unauthorized, ApiStatus.UNAUTHORIZED),
                StatusCase(HttpStatusCode.Forbidden, ApiStatus.UNAUTHORIZED),
                StatusCase(HttpStatusCode.NotFound, ApiStatus.NOT_FOUND),
                StatusCase(HttpStatusCode.Conflict, ApiStatus.CONFLICT),
                StatusCase(HttpStatusCode.PayloadTooLarge, ApiStatus.TOO_LARGE),
                StatusCase(HttpStatusCode.InsufficientStorage, ApiStatus.NO_SPACE),
                StatusCase(HttpStatusCode.InternalServerError, ApiStatus.FAILED),
            ) { case ->
                // given
                val server = Server("", case.status)

                // when
                val error = shouldThrow<ApiException> { server.api.quota() }

                // then
                error.status shouldBe case.expected
                error.message shouldBe case.expected.fallback
            }

            test("should use the message from the error body") {
                // given
                val server =
                    Server(
                        """{"error":"AlreadyExistsException","message":"File a.txt already exists","details":[]}""",
                        HttpStatusCode.Conflict,
                    )

                // when
                val error = shouldThrow<ApiException> { server.api.upload("", PickedFile("a.txt", byteArrayOf()), overwrite = false) }

                // then
                error.message shouldBe "File a.txt already exists"
            }

            test("should hide the server message of an unexpected failure") {
                // given
                val server = Server("""{"error":"InternalServerError","message":"Unexpected error"}""", HttpStatusCode.InternalServerError)

                // when
                val error = shouldThrow<ApiException> { server.api.trash() }

                // then
                error.message shouldBe ApiStatus.FAILED.fallback
            }

            test("should report an unreachable server as offline") {
                // given
                val api = HttpFilebrowserApi(HttpClient(MockEngine { throw IOException("Connection refused") }), BASE)

                // when
                val error = shouldThrow<ApiException> { api.quota() }

                // then
                error.status shouldBe ApiStatus.OFFLINE
            }

            test("should report an unreadable answer as a failure") {
                // given
                val server = Server("<html>")

                // when
                val error = shouldThrow<ApiException> { server.api.quota() }

                // then
                error.status shouldBe ApiStatus.FAILED
            }
        }
    })
