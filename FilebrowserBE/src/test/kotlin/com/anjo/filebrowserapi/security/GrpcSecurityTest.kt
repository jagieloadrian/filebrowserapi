package com.anjo.filebrowserapi.security

import com.anjo.filebrowser.Filebrowser
import com.anjo.filebrowser.FilebrowserApiGrpc
import com.anjo.filebrowser.FilebrowserApiGrpcKt
import com.anjo.filebrowserapi.TestObjects.captureOutput
import com.anjo.filebrowserapi.TestTokens
import com.anjo.filebrowserapi.TestTokens.READER
import com.anjo.filebrowserapi.TestTokens.WRITER
import com.google.protobuf.kotlin.toByteString
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientInterceptors
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.ClientCalls
import io.grpc.stub.MetadataUtils
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.flow.flowOf
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.nio.file.Files
import java.time.Instant
import java.util.concurrent.TimeUnit

@SpringBootTest(properties = ["spring.grpc.server.port=0"])
@ActiveProfiles("server")
@ApplyExtension(SpringExtension::class)
class GrpcSecurityTest : FunSpec() {
    @LocalGrpcServerPort
    private var port: Int = 0

    // ponytail: one channel per spec; the port is injected after construction, so it is built on first use
    private val channel by lazy { ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build() }

    init {
        afterSpec {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        }

        context("should return UNAUTHENTICATED when RPC is called without token") {
            withTests(allMethods()) { method ->
                // when & then
                call(method, null) shouldBe Status.Code.UNAUTHENTICATED
            }
        }

        context("should return PERMISSION_DENIED when token has no filebrowser role") {
            withTests(allMethods()) { method ->
                // when & then
                call(method, TestTokens.token("statistics-admin")) shouldBe Status.Code.PERMISSION_DENIED
            }
        }

        context("should return PERMISSION_DENIED when reader calls write RPC") {
            withTests(writeMethods()) { method ->
                // when & then
                call(method, TestTokens.token(READER)) shouldBe Status.Code.PERMISSION_DENIED
            }
        }

        context("should pass security when reader calls read RPC") {
            withTests(readMethods()) { method ->
                // when & then
                DENIED shouldNotContain call(method, TestTokens.token(READER))
            }
        }

        context("should pass security when writer calls any RPC") {
            withTests(allMethods()) { method ->
                // when & then
                DENIED shouldNotContain call(method, TestTokens.token(WRITER))
            }
        }

        context("should return UNAUTHENTICATED when token is invalid") {
            withTests(invalidTokens()) { token ->
                // when & then
                call(FilebrowserApiGrpc.getShowFilesTreeMethod().fullMethodName, token) shouldBe Status.Code.UNAUTHENTICATED
            }
        }

        test("should keep users apart when a file is streamed in") {
            // given
            val owner = FilebrowserApiGrpcKt.FilebrowserApiCoroutineStub(withToken(TestTokens.token(WRITER, subject = "alice")))
            val other = FilebrowserApiGrpcKt.FilebrowserApiCoroutineStub(withToken(TestTokens.token(WRITER, subject = "bob")))
            val file =
                Filebrowser.UploadFile
                    .newBuilder()
                    .setName("private.txt")
                    .setData("secret".toByteArray().toByteString())
                    .build()
            owner.uploadFile(flowOf(file))

            // when
            val ownView = owner.showFilesTree(Filebrowser.PathRequest.getDefaultInstance()).directoriesList
            val otherView = other.showFilesTree(Filebrowser.PathRequest.getDefaultInstance()).directoriesList

            // then
            ownView shouldContain "/private.txt"
            otherView shouldNotContain "/private.txt"
        }

        test("should never log the token when RPC is authorized") {
            // given
            val token = TestTokens.token(WRITER)

            // when
            val output =
                captureOutput {
                    call(FilebrowserApiGrpc.getShowFilesTreeMethod().fullMethodName, token)
                    call(FilebrowserApiGrpc.getDeleteFileMethod().fullMethodName, token)
                }

            // then
            output shouldNotContain token
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun call(
        fullMethodName: String,
        token: String?,
    ): Status.Code {
        val method =
            FilebrowserApiGrpc.getServiceDescriptor().methods.first { it.fullMethodName == fullMethodName }
                as MethodDescriptor<Any, Any>
        val request = (method.requestMarshaller as MethodDescriptor.PrototypeMarshaller<Any>).messagePrototype
        return try {
            ClientCalls.blockingUnaryCall(
                withToken(token).newCall(method, CallOptions.DEFAULT.withDeadlineAfter(5, TimeUnit.SECONDS)),
                request,
            )
            Status.Code.OK
        } catch (exception: StatusRuntimeException) {
            exception.status.code
        }
    }

    private fun withToken(token: String?): Channel =
        token?.let {
            val headers = Metadata().apply { put(AUTHORIZATION, "Bearer $it") }
            ClientInterceptors.intercept(channel, MetadataUtils.newAttachHeadersInterceptor(headers))
        } ?: channel

    companion object {
        private val AUTHORIZATION = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
        private val DENIED = listOf(Status.Code.UNAUTHENTICATED, Status.Code.PERMISSION_DENIED)
        private val READS =
            listOf(
                FilebrowserApiGrpc.getShowFilesTreeMethod(),
                FilebrowserApiGrpc.getGetFileMethod(),
                FilebrowserApiGrpc.getShowDirectoriesTreeMethod(),
                FilebrowserApiGrpc.getListEntriesMethod(),
                FilebrowserApiGrpc.getFindDuplicatesMethod(),
                FilebrowserApiGrpc.getListTrashMethod(),
                FilebrowserApiGrpc.getListVersionsMethod(),
                FilebrowserApiGrpc.getListSharesMethod(),
                FilebrowserApiGrpc.getGetQuotaMethod(),
            ).map { it.fullMethodName }

        private fun allMethods() = FilebrowserApiGrpc.getServiceDescriptor().methods.map { it.fullMethodName }

        private fun readMethods() = READS

        private fun writeMethods() = allMethods() - READS.toSet()

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
            registry.add("application.path") { Files.createTempDirectory("filebrowser-grpc-security").toString() }
            TestTokens.register(registry)
        }
    }
}
