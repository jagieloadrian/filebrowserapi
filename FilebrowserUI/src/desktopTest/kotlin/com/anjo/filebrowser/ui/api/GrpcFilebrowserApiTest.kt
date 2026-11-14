package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.Filebrowser
import com.anjo.filebrowser.FilebrowserApiGrpcKt.FilebrowserApiCoroutineImplBase
import com.anjo.filebrowser.FilebrowserApiGrpcKt.FilebrowserApiCoroutineStub
import com.anjo.filebrowser.entriesResponse
import com.anjo.filebrowser.entry
import com.anjo.filebrowser.ui.file
import com.anjo.filebrowser.ui.folder
import com.anjo.filebrowser.ui.model.PickedFile
import com.google.protobuf.Empty
import com.google.protobuf.timestamp
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlin.time.Instant

private data class CodeCase(
    val code: Status.Code,
    val expected: ApiStatus,
)

class GrpcFilebrowserApiTest :
    FunSpec({
        val received = mutableListOf<Any>()
        val uploaded = mutableListOf<Filebrowser.UploadFile>()
        var failure = Status.OK
        val service =
            object : FilebrowserApiCoroutineImplBase() {
                override suspend fun listEntries(request: Filebrowser.EntriesRequest): Filebrowser.EntriesResponse {
                    received += request
                    return entriesResponse {
                        entries +=
                            entry {
                                path = "/Docs/a.txt"
                                size = 4
                                modified =
                                    timestamp {
                                        seconds = 1_794_564_000
                                        nanos = 500_000_000
                                    }
                            }
                    }
                }

                override suspend fun uploadFile(requests: Flow<Filebrowser.UploadFile>): Filebrowser.StringResponse {
                    uploaded += requests.toList()
                    return Filebrowser.StringResponse.getDefaultInstance()
                }

                override suspend fun deleteFile(request: Filebrowser.FileOperation): Empty {
                    received += request
                    return Empty.getDefaultInstance()
                }

                override suspend fun deleteDirectoryRecursively(request: Filebrowser.PathRequest): Empty {
                    received += request
                    return Empty.getDefaultInstance()
                }

                override suspend fun getQuota(request: Empty): Filebrowser.Quota = throw StatusException(failure)
            }
        val name = InProcessServerBuilder.generateName()
        val server =
            InProcessServerBuilder
                .forName(name)
                .directExecutor()
                .addService(service)
                .build()
                .start()
        val channel = InProcessChannelBuilder.forName(name).directExecutor().build()
        val api = GrpcFilebrowserApi(FilebrowserApiCoroutineStub(channel))

        beforeTest {
            received.clear()
            uploaded.clear()
        }

        afterSpec {
            channel.shutdownNow()
            server.shutdownNow()
        }

        test("should list entries by path and name and map the timestamp") {
            // when
            val entries = api.listEntries("/Docs", "a")

            // then
            val request = received.single() as Filebrowser.EntriesRequest
            request.path shouldBe "/Docs"
            request.name shouldBe "a"
            entries shouldBe listOf(file("/Docs/a.txt", size = 4).copy(modified = Instant.parse("2026-11-13T10:00:00.5Z")))
        }

        test("should stream the upload in chunks that carry the target") {
            // given
            val bytes = ByteArray(64 * 1024 + 1) { 7 }

            // when
            api.upload("/Docs", PickedFile("big.bin", bytes), overwrite = true)

            // then
            uploaded.map { it.data.size() } shouldBe listOf(64 * 1024, 1)
            uploaded.first().name shouldBe "big.bin"
            uploaded.first().path shouldBe "/Docs"
            uploaded.first().overwrite shouldBe true
        }

        test("should move files and whole folders to the trash") {
            // when
            api.delete(file("/Docs/a.txt"))
            api.delete(folder("/Docs"))

            // then
            (received[0] as Filebrowser.FileOperation).let { it.name to it.path } shouldBe ("a.txt" to "/Docs")
            (received[1] as Filebrowser.PathRequest).path shouldBe "/Docs"
        }

        context("errors") {
            withTests(
                nameFn = { "should map ${it.code} to ${it.expected}" },
                CodeCase(Status.Code.INVALID_ARGUMENT, ApiStatus.INVALID),
                CodeCase(Status.Code.UNAUTHENTICATED, ApiStatus.UNAUTHORIZED),
                CodeCase(Status.Code.NOT_FOUND, ApiStatus.NOT_FOUND),
                CodeCase(Status.Code.ALREADY_EXISTS, ApiStatus.CONFLICT),
                CodeCase(Status.Code.FAILED_PRECONDITION, ApiStatus.CONFLICT),
                CodeCase(Status.Code.RESOURCE_EXHAUSTED, ApiStatus.NO_SPACE),
                CodeCase(Status.Code.UNKNOWN, ApiStatus.FAILED),
            ) { case ->
                // given
                failure = Status.fromCode(case.code)

                // when
                val error = shouldThrow<ApiException> { api.quota() }

                // then
                error.status shouldBe case.expected
            }

            test("should use the status description as the message") {
                // given
                failure = Status.RESOURCE_EXHAUSTED.withDescription("Upload exceeds the maximum upload size")

                // when
                val error = shouldThrow<ApiException> { api.quota() }

                // then
                error.message shouldBe "Upload exceeds the maximum upload size"
            }

            test("should report an unreachable server as offline") {
                // given
                val missing = InProcessChannelBuilder.forName("missing").build()

                // when
                val error = shouldThrow<ApiException> { GrpcFilebrowserApi(FilebrowserApiCoroutineStub(missing)).quota() }

                // then
                error.status shouldBe ApiStatus.OFFLINE
                missing.shutdownNow()
            }
        }
    })
