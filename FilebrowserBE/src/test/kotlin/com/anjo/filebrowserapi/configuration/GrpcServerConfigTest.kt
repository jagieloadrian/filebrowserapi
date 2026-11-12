package com.anjo.filebrowserapi.configuration

import com.anjo.filebrowser.Filebrowser
import com.anjo.filebrowser.FilebrowserApiGrpc
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.CannotRealizeException
import com.anjo.filebrowserapi.exception.GrpcGlobalControllerAdvice
import com.anjo.filebrowserapi.grpc.FilebrowserGrpc
import com.anjo.filebrowserapi.logging.GrpcLoggingInterceptor
import com.anjo.filebrowserapi.service.DirectoryManager
import com.anjo.filebrowserapi.service.FileManager
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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
            FilebrowserGrpc(mockk<DirectoryManager>(), fileManager),
            GrpcLoggingInterceptor(),
            GrpcGlobalControllerAdvice(),
        ).apply { run(null) }
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
    fun `should skip shutdown when server was never started`() {
        // given
        val input =
            GrpcServerConfig(
                GrpcServerProperties(port),
                FilebrowserGrpc(mockk<DirectoryManager>(), fileManager),
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
                Arguments.of(CannotRealizeException(TEST_MESSAGE), Status.Code.DATA_LOSS, TEST_MESSAGE),
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
