package com.anjo.filebrowserapi.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ForwardingServerCall
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import org.springframework.stereotype.Component

@Component
class GrpcGlobalControllerAdvice : ServerInterceptor {
    private val logger = KotlinLogging.logger {}

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>?,
        headers: Metadata?,
        next: ServerCallHandler<ReqT, RespT>?,
    ): ServerCall.Listener<ReqT> =
        object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
            next?.startCall(
                object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    override fun close(
                        status: Status,
                        trailers: Metadata,
                    ) {
                        if (!status.isOk) {
                            val newStatus = status.cause?.let { getStatus(it, call) } ?: status
                            logger.error(newStatus.cause) { "gRPC call failed: ${newStatus.description}" }
                            super.close(newStatus, trailers)
                        } else {
                            super.close(status, trailers)
                        }
                    }
                },
                headers,
            ),
        ) {
            override fun onHalfClose() {
                try {
                    super.onHalfClose()
                } catch (exc: Exception) {
                    handleEndpointException(exc, call)
                }
            }
        }

    private fun <ReqT, RespT> handleEndpointException(
        exception: Exception,
        call: ServerCall<ReqT, RespT>?,
    ) {
        val status = getStatus(exception, call)
        call?.close(status, Metadata())
    }

    private fun <ReqT, RespT> getStatus(
        exception: Throwable,
        call: ServerCall<ReqT, RespT>?,
    ): Status? {
        logger.error(exception) { "Exception during gRPC endpoint call: ${call?.methodDescriptor?.fullMethodName}" }
        val status =
            when (exception) {
                is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(exception.message)
                is IllegalStateException -> Status.FAILED_PRECONDITION.withDescription(exception.message)
                is FileBrowserException -> grpcStatus(exception).withDescription(exception.message)
                else ->
                    Status.UNKNOWN
                        .withDescription("Unexpected error occurred")
                        .withCause(exception)
            }
        return status
    }

    private fun grpcStatus(exception: FileBrowserException): Status =
        when (exception) {
            is BadInputException -> Status.INVALID_ARGUMENT
            is NotFoundException -> Status.NOT_FOUND
            is AlreadyExistsException -> Status.ALREADY_EXISTS
            is NotEmptyException -> Status.FAILED_PRECONDITION
        }
}
