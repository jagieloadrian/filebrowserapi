package com.anjo.filebrowser.ui.api

import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.decodeURLPart

private const val FRAME_HEADER = 5
private const val TRAILER_FLAG = 0x80
private const val BYTE_MASK = 0xFF
internal const val GRPC_STATUS = "grpc-status"
private const val GRPC_MESSAGE = "grpc-message"

internal fun grpcWebFrame(message: ByteArray): ByteArray =
    byteArrayOf(0) + (3 downTo 0).map { (message.size ushr (it * Byte.SIZE_BITS)).toByte() } + message

internal fun grpcWebMessages(
    status: HttpStatusCode,
    headers: Headers,
    body: ByteArray,
): List<ByteArray> {
    val messages = mutableListOf<ByteArray>()
    var trailers = emptyMap<String, String>()
    var offset = 0
    while (offset < body.size) {
        if (body.size - offset < FRAME_HEADER) throw ApiException(ApiStatus.FAILED)
        val length = (1..4).fold(0) { acc, i -> (acc shl Byte.SIZE_BITS) or (body[offset + i].toInt() and BYTE_MASK) }
        val start = offset + FRAME_HEADER
        if (length < 0 || length > body.size - start) throw ApiException(ApiStatus.FAILED)
        val payload = body.copyOfRange(start, start + length)
        if (body[offset].toInt() and TRAILER_FLAG == 0) messages += payload else trailers = trailersOf(payload)
        offset = start + length
    }
    val code = (trailers[GRPC_STATUS] ?: headers[GRPC_STATUS])?.trim()?.toIntOrNull()
    return when (code) {
        GRPC_OK -> messages
        null -> throw ApiException(apiStatusOfHttp(status))
        else -> throw ApiException(apiStatusOfGrpc(code), (trailers[GRPC_MESSAGE] ?: headers[GRPC_MESSAGE])?.decodeURLPart())
    }
}

private fun trailersOf(payload: ByteArray): Map<String, String> =
    payload
        .decodeToString()
        .split("\r\n")
        .filter { ':' in it }
        .associate { it.substringBefore(':').trim().lowercase() to it.substringAfter(':').trim() }

private fun apiStatusOfHttp(status: HttpStatusCode): ApiStatus =
    when (status) {
        HttpStatusCode.Unauthorized -> ApiStatus.SIGNED_OUT

        HttpStatusCode.Forbidden -> ApiStatus.FORBIDDEN

        HttpStatusCode.TooManyRequests,
        HttpStatusCode.BadGateway,
        HttpStatusCode.ServiceUnavailable,
        HttpStatusCode.GatewayTimeout,
        -> ApiStatus.OFFLINE

        else -> ApiStatus.FAILED
    }
