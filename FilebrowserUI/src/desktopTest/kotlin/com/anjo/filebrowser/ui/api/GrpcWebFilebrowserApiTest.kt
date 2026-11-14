package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.EntriesRequest
import com.anjo.filebrowser.EntriesResponse
import com.anjo.filebrowser.FileOperation
import com.anjo.filebrowser.FileResponse
import com.anjo.filebrowser.PathRequest
import com.anjo.filebrowser.TrashId
import com.anjo.filebrowser.TrashResponse
import com.anjo.filebrowser.ui.file
import com.anjo.filebrowser.ui.folder
import com.anjo.filebrowser.ui.model.PickedFile
import com.anjo.filebrowser.ui.model.Quota
import com.anjo.filebrowser.ui.trashed
import com.squareup.wire.Message
import com.squareup.wire.ofEpochSecond
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
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.io.IOException
import okio.ByteString
import okio.ByteString.Companion.toByteString
import kotlin.time.Instant
import com.anjo.filebrowser.Entry as EntryMessage
import com.anjo.filebrowser.Quota as QuotaMessage
import com.anjo.filebrowser.TrashItem as TrashItemMessage

private const val BASE = "http://localhost:8090"
private const val SERVICE = "/com.anjo.filebrowser.FilebrowserApi"
private val TEN_O_CLOCK = ofEpochSecond(1_794_564_000, 0)

private fun answer(vararg messages: Message<*, *>): ByteArray =
    messages.fold(byteArrayOf()) { body, message -> body + grpcWebFrame(message.encode()) } + OK_TRAILER

private class GrpcWebServer(
    private val body: ByteArray = OK_TRAILER,
    private val headers: Headers = Headers.Empty,
    private val status: HttpStatusCode = HttpStatusCode.OK,
) {
    val requests = mutableListOf<HttpRequestData>()
    val api =
        GrpcWebFilebrowserApi(
            HttpClient(
                MockEngine { request ->
                    requests += request
                    respond(body, status, headers)
                },
            ),
            BASE,
        )
    val request get() = requests.single()
}

private suspend fun HttpRequestData.message(): ByteString =
    body
        .toByteArray()
        .drop(5)
        .toByteArray()
        .toByteString()

private data class GrpcCodeCase(
    val code: Int,
    val expected: ApiStatus,
)

class GrpcWebFilebrowserApiTest :
    FunSpec({
        context("reading") {
            test("should post a framed listEntries request with the gRPC-Web headers and map the entries") {
                // given
                val server =
                    GrpcWebServer(
                        answer(
                            EntriesResponse(
                                entries =
                                    listOf(
                                        EntryMessage(path = "/Docs/a.txt", size = 4, modified = ofEpochSecond(1_794_564_000, 500_000_000)),
                                    ),
                            ),
                        ),
                    )

                // when
                val entries = server.api.listEntries("/Docs", "a")

                // then
                server.request.method shouldBe HttpMethod.Post
                server.request.url.toString() shouldBe "$BASE$SERVICE/listEntries"
                server.request.headers["x-grpc-web"] shouldBe "1"
                server.request.body.contentType shouldBe ContentType("application", "grpc-web+proto")
                server.request.message() shouldBe EntriesRequest(path = "/Docs", name = "a").encodeByteString()
                entries shouldBe listOf(file("/Docs/a.txt", size = 4).copy(modified = Instant.parse("2026-11-13T10:00:00.5Z")))
            }

            test("should join every streamed chunk of a download") {
                // given
                val server =
                    GrpcWebServer(
                        answer(
                            FileResponse("hel".encodeToByteArray().toByteString()),
                            FileResponse("lo".encodeToByteArray().toByteString()),
                        ),
                    )

                // when
                val bytes = server.api.download(file("/Docs/a.txt"))

                // then
                server.request.url.encodedPath shouldBe "$SERVICE/getFile"
                server.request.message() shouldBe FileOperation(name = "a.txt", path = "/Docs").encodeByteString()
                bytes.decodeToString() shouldBe "hello"
            }

            test("should map the trash and the quota") {
                // given
                val trash =
                    GrpcWebServer(
                        answer(TrashResponse(listOf(TrashItemMessage(id = "7", path = "/a.txt", size = 1, deleted_at = TEN_O_CLOCK)))),
                    )
                val quota = GrpcWebServer(answer(QuotaMessage(used = 5, limit = 10)))

                // when & then
                trash.api.trash() shouldBe listOf(trashed("7", "/a.txt"))
                trash.request.url.encodedPath shouldBe "$SERVICE/listTrash"
                trash.request.message() shouldBe ByteString.EMPTY
                quota.api.quota() shouldBe Quota(used = 5, limit = 10)
                quota.request.url.encodedPath shouldBe "$SERVICE/getQuota"
            }
        }

        context("writing") {
            test("should upload over REST because gRPC-Web cannot stream from the client") {
                // given
                val server = GrpcWebServer(byteArrayOf())

                // when
                server.api.upload("/Docs", PickedFile("a.txt", "data".encodeToByteArray()), overwrite = true)

                // then
                server.request.method shouldBe HttpMethod.Post
                server.request.url.toString() shouldBe "$BASE/api/v1/files?path=%2FDocs&overwrite=true"
                server.request.body
                    .toByteArray()
                    .decodeToString() shouldContain """form-data; name="uploadFile"; filename="a.txt""""
            }

            test("should send each change to its RPC with the proto request") {
                // given
                val server = GrpcWebServer()
                val item = trashed("7", "/a.txt")

                // when
                server.api.createFolder("/Docs", "Notes")
                server.api.rename(file("/Docs/a.txt"), "b.txt")
                server.api.rename(folder("/Docs"), "Papers")
                server.api.delete(file("/Docs/a.txt"))
                server.api.delete(folder("/Docs"))
                server.api.restore(item)
                server.api.deleteForever(item)
                server.api.emptyTrash()

                // then
                server.requests.map { it.url.encodedPath.removePrefix("$SERVICE/") to it.message() } shouldBe
                    listOf(
                        "addDirectory" to FileOperation(name = "Notes", path = "/Docs").encodeByteString(),
                        "renameFile" to FileOperation(name = "b.txt", path = "/Docs/a.txt").encodeByteString(),
                        "renameDirectory" to FileOperation(name = "Papers", path = "/Docs").encodeByteString(),
                        "deleteFile" to FileOperation(name = "a.txt", path = "/Docs").encodeByteString(),
                        "deleteDirectoryRecursively" to PathRequest(path = "/Docs").encodeByteString(),
                        "restoreTrash" to TrashId(id = "7").encodeByteString(),
                        "purgeTrash" to TrashId(id = "7").encodeByteString(),
                        "emptyTrash" to ByteString.EMPTY,
                    )
            }
        }

        context("errors") {
            withTests(
                nameFn = { "should map gRPC status ${it.code} to ${it.expected}" },
                GrpcCodeCase(3, ApiStatus.INVALID),
                GrpcCodeCase(16, ApiStatus.UNAUTHORIZED),
                GrpcCodeCase(7, ApiStatus.UNAUTHORIZED),
                GrpcCodeCase(5, ApiStatus.NOT_FOUND),
                GrpcCodeCase(6, ApiStatus.CONFLICT),
                GrpcCodeCase(9, ApiStatus.CONFLICT),
                GrpcCodeCase(8, ApiStatus.NO_SPACE),
                GrpcCodeCase(14, ApiStatus.OFFLINE),
                GrpcCodeCase(2, ApiStatus.FAILED),
            ) { case ->
                // given
                val server = GrpcWebServer(trailerFrame("grpc-status:${case.code}\r\n"))

                // when
                val error = shouldThrow<ApiException> { server.api.quota() }

                // then
                error.status shouldBe case.expected
            }

            test("should use the message of a trailers-only answer") {
                // given
                val server =
                    GrpcWebServer(byteArrayOf(), headersOf("grpc-status" to listOf("8"), "grpc-message" to listOf("Upload%20too%20large")))

                // when
                val error = shouldThrow<ApiException> { server.api.createFolder("", "a") }

                // then
                error.status shouldBe ApiStatus.NO_SPACE
                error.message shouldBe "Upload too large"
            }

            test("should report an unreachable proxy as offline") {
                // given
                val api = GrpcWebFilebrowserApi(HttpClient(MockEngine { throw IOException("Connection refused") }), BASE)

                // when
                val error = shouldThrow<ApiException> { api.quota() }

                // then
                error.status shouldBe ApiStatus.OFFLINE
            }

            test("should report an undecodable message as a failure") {
                // given
                val server = GrpcWebServer(grpcWebFrame(byteArrayOf(0x0A, 0x05)) + OK_TRAILER)

                // when
                val error = shouldThrow<ApiException> { server.api.quota() }

                // then
                error.status shouldBe ApiStatus.FAILED
            }

            test("should report a unary answer without a message as a failure") {
                // given
                val server = GrpcWebServer()

                // when
                val error = shouldThrow<ApiException> { server.api.trash() }

                // then
                error.status shouldBe ApiStatus.FAILED
            }
        }
    })
