package com.anjo.filebrowserapi.configuration

import com.anjo.filebrowser.Filebrowser
import com.anjo.filebrowser.FilebrowserApiGrpc
import com.anjo.filebrowser.FilebrowserApiGrpcKt
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.exception.AlreadyExistsException
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.GrpcGlobalControllerAdvice
import com.anjo.filebrowserapi.exception.NotEmptyException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.grpc.FilebrowserGrpc
import com.anjo.filebrowserapi.logging.GrpcLoggingInterceptor
import com.anjo.filebrowserapi.service.DirectoryManager
import com.anjo.filebrowserapi.service.FileManager
import com.google.protobuf.kotlin.toByteString
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.DefaultApplicationArguments
import java.io.InputStream
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test

class GrpcServerConfigTest {
    private val fileManager = mockk<FileManager>()
    private val port = ServerSocket(0).use { it.localPort }
    private val config =
        GrpcServerConfig(
            GrpcServerProperties(port),
            FilebrowserGrpc(mockk<DirectoryManager>(), fileManager, AppConfiguration()),
            GrpcLoggingInterceptor(),
            GrpcGlobalControllerAdvice(),
        ).apply { run(DefaultApplicationArguments()) }
    private val channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build()
    private val stub = FilebrowserApiGrpc.newBlockingStub(channel)

    @AfterTest
    fun stop() {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        config.shutdown()
    }

    @Test
    fun `should serve files tree when server is started`() {
        // given
        val expected = listOf("/$FIRST_NAME")
        every { fileManager.showFilesTree(FIRST_NAME) } returns expected

        // when
        val result = stub.withDeadlineAfter(5, TimeUnit.SECONDS).showFilesTree(request())

        // then
        result.directoriesList shouldBe expected
    }

    @Test
    fun `should map exception to status when streaming getFile fails`() {
        // given
        every { fileManager.getFile(FIRST_NAME, FIRST_NAME) } throws NotFoundException(TEST_MESSAGE)
        val request =
            Filebrowser.FileOperation
                .newBuilder()
                .setName(FIRST_NAME)
                .setPath(FIRST_NAME)
                .build()

        // when
        val result =
            shouldThrow<StatusRuntimeException> {
                stub.withDeadlineAfter(5, TimeUnit.SECONDS).getFile(request).next()
            }

        // then
        result.status.code shouldBe Status.Code.NOT_FOUND
        result.status.description shouldBe TEST_MESSAGE
    }

    @Test
    fun `should upload chunks over the wire when client streams file`() =
        runTest {
            // given
            var received = ""
            every { fileManager.uploadFile(FIRST_NAME, any(), "", false) } answers {
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
            val result = FilebrowserApiGrpcKt.FilebrowserApiCoroutineStub(channel).uploadFile(chunks.asFlow())

            // then
            result.path shouldBe "/$FIRST_NAME"
            received shouldBe TEST_MESSAGE
        }

    @Test
    fun `should skip shutdown when server was never started`() {
        // given
        val input =
            GrpcServerConfig(
                GrpcServerProperties(port),
                FilebrowserGrpc(mockk<DirectoryManager>(), fileManager, AppConfiguration()),
                GrpcLoggingInterceptor(),
                GrpcGlobalControllerAdvice(),
            )

        // when & then
        shouldNotThrowAny { input.shutdown() }
    }

    @ParameterizedTest
    @MethodSource("errors")
    fun `should map exception to status when service throws`(
        exception: Exception,
        expected: Status.Code,
        expectedDescription: String,
    ) {
        // given
        every { fileManager.showFilesTree(FIRST_NAME) } throws exception

        // when
        val result =
            shouldThrow<StatusRuntimeException> {
                stub.withDeadlineAfter(5, TimeUnit.SECONDS).showFilesTree(request())
            }

        // then
        result.status.code shouldBe expected
        result.status.description shouldBe expectedDescription
    }

    private fun request() =
        Filebrowser.PathRequest
            .newBuilder()
            .setPath(FIRST_NAME)
            .build()

    companion object {
        @JvmStatic
        fun errors() =
            listOf(
                Arguments.of(NotFoundException(TEST_MESSAGE), Status.Code.NOT_FOUND, TEST_MESSAGE),
                Arguments.of(AlreadyExistsException(TEST_MESSAGE), Status.Code.ALREADY_EXISTS, TEST_MESSAGE),
                Arguments.of(NotEmptyException(TEST_MESSAGE), Status.Code.FAILED_PRECONDITION, TEST_MESSAGE),
                Arguments.of(BadInputException(TEST_MESSAGE), Status.Code.INVALID_ARGUMENT, TEST_MESSAGE),
                Arguments.of(IllegalArgumentException(TEST_MESSAGE), Status.Code.INVALID_ARGUMENT, TEST_MESSAGE),
                Arguments.of(IllegalStateException(TEST_MESSAGE), Status.Code.FAILED_PRECONDITION, TEST_MESSAGE),
                Arguments.of(RuntimeException(TEST_MESSAGE), Status.Code.UNKNOWN, "Unexpected error occurred"),
                Arguments.of(
                    StatusException(Status.NOT_FOUND.withDescription(TEST_MESSAGE)),
                    Status.Code.NOT_FOUND,
                    TEST_MESSAGE,
                ),
            )
    }
}
