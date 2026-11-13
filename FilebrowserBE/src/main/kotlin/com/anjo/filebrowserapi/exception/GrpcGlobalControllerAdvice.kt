package com.anjo.filebrowserapi.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.grpc.server.exception.GrpcExceptionHandler
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Component

@Component
class GrpcGlobalControllerAdvice : GrpcExceptionHandler {
    private val logger = KotlinLogging.logger {}

    override fun handleException(exception: Throwable): StatusException? =
        when (exception) {
            is AuthenticationException, is AccessDeniedException -> {
                null
            }

            is IllegalArgumentException -> {
                Status.INVALID_ARGUMENT.withDescription(exception.message)
            }

            is IllegalStateException -> {
                Status.FAILED_PRECONDITION.withDescription(exception.message)
            }

            is FileBrowserException -> {
                grpcStatus(exception).withDescription(exception.message)
            }

            else -> {
                logger.error(exception) { "Unexpected gRPC error" }
                Status.UNKNOWN.withDescription("Unexpected error occurred")
            }
        }?.asException()

    private fun grpcStatus(exception: FileBrowserException): Status =
        when (exception) {
            is BadInputException -> Status.INVALID_ARGUMENT
            is NotFoundException -> Status.NOT_FOUND
            is AlreadyExistsException -> Status.ALREADY_EXISTS
            is NotEmptyException -> Status.FAILED_PRECONDITION
            is QuotaExceededException -> Status.RESOURCE_EXHAUSTED
        }
}
