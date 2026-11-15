package com.anjo.filebrowser.ui.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

internal fun trailerFrame(trailers: String): ByteArray = grpcWebFrame(trailers.encodeToByteArray()).also { it[0] = 0x80.toByte() }

internal val OK_TRAILER = trailerFrame("grpc-status:0\r\ngrpc-message:\r\n")

private data class HttpCase(
    val status: HttpStatusCode,
    val expected: ApiStatus,
)

private data class BodyCase(
    val name: String,
    val body: ByteArray,
)

class GrpcWebTest :
    FunSpec({
        test("should prefix the message with flag 0 and its big-endian length") {
            // when
            val frame = grpcWebFrame(ByteArray(258) { 7 })

            // then
            frame.take(5) shouldBe listOf<Byte>(0, 0, 0, 1, 2)
            frame.size shouldBe 263
        }

        test("should return every data frame when the trailer reports OK") {
            // given
            val body =
                grpcWebFrame("a".encodeToByteArray()) + grpcWebFrame(byteArrayOf()) + grpcWebFrame("bc".encodeToByteArray()) + OK_TRAILER

            // when
            val messages = grpcWebMessages(HttpStatusCode.OK, Headers.Empty, body)

            // then
            messages.map { it.decodeToString() } shouldBe listOf("a", "", "bc")
        }

        test("should read the status and the percent-encoded message from the trailer") {
            // given
            val body = trailerFrame("Grpc-Status: 5\r\ngrpc-message: File%20a.txt%20not%20found\r\n")

            // when
            val error = shouldThrow<ApiException> { grpcWebMessages(HttpStatusCode.OK, Headers.Empty, body) }

            // then
            error.status shouldBe ApiStatus.NOT_FOUND
            error.message shouldBe "File a.txt not found"
        }

        test("should read the status from the headers of a trailers-only answer") {
            // given
            val headers = headersOf("grpc-status" to listOf("6"), "grpc-message" to listOf("File%20exists"))

            // when
            val error = shouldThrow<ApiException> { grpcWebMessages(HttpStatusCode.OK, headers, byteArrayOf()) }

            // then
            error.status shouldBe ApiStatus.CONFLICT
            error.message shouldBe "File exists"
        }

        context("without a gRPC status") {
            withTests(
                nameFn = { "should map HTTP ${it.status.value} to ${it.expected}" },
                HttpCase(HttpStatusCode.ServiceUnavailable, ApiStatus.OFFLINE),
                HttpCase(HttpStatusCode.BadGateway, ApiStatus.OFFLINE),
                HttpCase(HttpStatusCode.Unauthorized, ApiStatus.SIGNED_OUT),
                HttpCase(HttpStatusCode.Forbidden, ApiStatus.FORBIDDEN),
                HttpCase(HttpStatusCode.NotFound, ApiStatus.FAILED),
                HttpCase(HttpStatusCode.OK, ApiStatus.FAILED),
            ) { case ->
                // when
                val error = shouldThrow<ApiException> { grpcWebMessages(case.status, Headers.Empty, byteArrayOf()) }

                // then
                error.status shouldBe case.expected
            }
        }

        context("malformed bodies") {
            withTests(
                nameFn = { "should fail on ${it.name}" },
                BodyCase("a cut frame header", byteArrayOf(0, 0, 0)),
                BodyCase("a length beyond the body", grpcWebFrame("abc".encodeToByteArray()).copyOf(6)),
                BodyCase("a negative length", byteArrayOf(0, -1, -1, -1, -1)),
            ) { case ->
                // when
                val error = shouldThrow<ApiException> { grpcWebMessages(HttpStatusCode.OK, Headers.Empty, case.body) }

                // then
                error.status shouldBe ApiStatus.FAILED
            }
        }
    })
