package com.anjo.filebrowserapi.grpc

import com.anjo.filebrowser.Filebrowser
import com.anjo.filebrowser.FilebrowserApiGrpc
import com.anjo.filebrowser.FilebrowserApiGrpcKt
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.TestObjects.captureOutput
import com.anjo.filebrowserapi.TestTokens
import com.anjo.filebrowserapi.exception.AlreadyExistsException
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotEmptyException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.exception.QuotaExceededException
import com.anjo.filebrowserapi.service.FileManager
import com.google.protobuf.kotlin.toByteString
import com.ninjasquad.springmockk.MockkBean
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.health.v1.HealthGrpc
import io.grpc.stub.MetadataUtils
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import kotlinx.coroutines.flow.asFlow
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.TimeUnit

private const val TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736"

@SpringBootTest(properties = ["spring.grpc.server.port=0"])
@ActiveProfiles("server")
@ApplyExtension(SpringExtension::class)
class GrpcServerTest : FunSpec() {
    @LocalGrpcServerPort
    private var port: Int = 0

    @MockkBean
    private lateinit var fileManager: FileManager

    // ponytail: one channel per spec; the port is injected after construction, so it is built on first use
    private val channel by lazy { ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build() }

    // getters, so every test gets a fresh token and a fresh 5 s deadline
    private val writer
        get() =
            MetadataUtils.newAttachHeadersInterceptor(
                Metadata().apply {
                    put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer ${TestTokens.token(TestTokens.WRITER)}")
                },
            )
    private val stub
        get() =
            FilebrowserApiGrpc
                .newBlockingStub(channel)
                .withInterceptors(writer)
                .withDeadlineAfter(5, TimeUnit.SECONDS)

    init {
        afterSpec {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        }

        test("should serve files tree when server is started") {
            // given
            every { fileManager.showFilesTree(FIRST_NAME) } returns listOf("/$FIRST_NAME")

            // when
            val result = stub.showFilesTree(pathRequest())

            // then
            result.directoriesList shouldBe listOf("/$FIRST_NAME")
        }

        context("should map exception to status when service throws") {
            withTests(nameFn = { it.first::class.simpleName!! }, errors()) { (exception, expected, expectedDescription) ->
                // given
                every { fileManager.showFilesTree(FIRST_NAME) } throws exception

                // when
                val result = shouldThrow<StatusRuntimeException> { stub.showFilesTree(pathRequest()) }

                // then
                result.status.code shouldBe expected
                result.status.description shouldBe expectedDescription
            }
        }

        test("should map exception to status when streaming getFile fails") {
            // given
            every { fileManager.getFile(FIRST_NAME, FIRST_NAME) } throws NotFoundException(TEST_MESSAGE)
            val request =
                Filebrowser.FileOperation
                    .newBuilder()
                    .setName(FIRST_NAME)
                    .setPath(FIRST_NAME)
                    .build()

            // when
            val result = shouldThrow<StatusRuntimeException> { stub.getFile(request).next() }

            // then
            result.status.code shouldBe Status.Code.NOT_FOUND
            result.status.description shouldBe TEST_MESSAGE
        }

        test("should upload chunks over the wire when client streams file") {
            // given
            var received = ""
            every { fileManager.uploadFile(FIRST_NAME, any(), any(), "", false) } answers {
                received = secondArg<InputStream>().readBytes().decodeToString()
                "/$FIRST_NAME"
            }
            val chunks =
                listOf("Test", "Message").mapIndexed { index, text ->
                    Filebrowser.UploadFile
                        .newBuilder()
                        .apply { if (index == 0) name = FIRST_NAME }
                        .setData(text.toByteArray().toByteString())
                        .build()
                }

            // when
            val result = FilebrowserApiGrpcKt.FilebrowserApiCoroutineStub(channel).withInterceptors(writer).uploadFile(chunks.asFlow())

            // then
            result.path shouldBe "/$FIRST_NAME"
            received shouldBe TEST_MESSAGE
        }

        test("should report SERVING when health is checked") {
            // when
            val result =
                HealthGrpc
                    .newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .check(HealthCheckRequest.getDefaultInstance())

            // then
            result.status shouldBe HealthCheckResponse.ServingStatus.SERVING
        }

        test("should log trace id from traceparent when call is received") {
            // given
            every { fileManager.showFilesTree(FIRST_NAME) } returns emptyList()
            val headers =
                Metadata().apply {
                    put(Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER), "00-$TRACE_ID-00f067aa0ba902b7-01")
                }

            // when
            val output =
                captureOutput {
                    stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers)).showFilesTree(pathRequest())
                }

            // then
            output shouldContain TRACE_ID
        }
    }

    private fun pathRequest() =
        Filebrowser.PathRequest
            .newBuilder()
            .setPath(FIRST_NAME)
            .build()

    companion object {
        private fun errors() =
            listOf(
                Triple(NotFoundException(TEST_MESSAGE), Status.Code.NOT_FOUND, TEST_MESSAGE),
                Triple(AlreadyExistsException(TEST_MESSAGE), Status.Code.ALREADY_EXISTS, TEST_MESSAGE),
                Triple(NotEmptyException(TEST_MESSAGE), Status.Code.FAILED_PRECONDITION, TEST_MESSAGE),
                Triple(QuotaExceededException(TEST_MESSAGE), Status.Code.RESOURCE_EXHAUSTED, TEST_MESSAGE),
                Triple(BadInputException(TEST_MESSAGE), Status.Code.INVALID_ARGUMENT, TEST_MESSAGE),
                Triple(IllegalArgumentException(TEST_MESSAGE), Status.Code.INVALID_ARGUMENT, TEST_MESSAGE),
                Triple(IllegalStateException(TEST_MESSAGE), Status.Code.FAILED_PRECONDITION, TEST_MESSAGE),
                Triple(RuntimeException(TEST_MESSAGE), Status.Code.UNKNOWN, "Unexpected error occurred"),
                Triple(
                    StatusException(Status.NOT_FOUND.withDescription(TEST_MESSAGE)),
                    Status.Code.NOT_FOUND,
                    TEST_MESSAGE,
                ),
            )

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            val workDir = Files.createTempDirectory("filebrowser-grpc")
            registry.add("application.path") { workDir.toString() }
            TestTokens.register(registry)
        }
    }
}
