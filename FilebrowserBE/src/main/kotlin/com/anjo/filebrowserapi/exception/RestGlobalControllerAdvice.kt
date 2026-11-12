package com.anjo.filebrowserapi.exception

import com.anjo.filebrowserapi.model.ApiError
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException

@RestControllerAdvice
@Profile("web")
class RestGlobalControllerAdvice {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(FileBrowserException::class)
    fun handleFileBrowserException(exception: FileBrowserException) =
        apiError(
            when (exception) {
                is BadInputException -> HttpStatus.BAD_REQUEST
                is NotFoundException -> HttpStatus.NOT_FOUND
                is AlreadyExistsException, is NotEmptyException -> HttpStatus.CONFLICT
            },
            exception.javaClass.simpleName,
            exception.message,
        )

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleValidationException(exception: HandlerMethodValidationException) =
        apiError(
            exception.statusCode,
            exception.javaClass.simpleName,
            exception.body.detail,
            exception.parameterValidationResults.flatMap { result ->
                result.resolvableErrors.map { "${result.methodParameter.parameterName}: ${it.defaultMessage}" }
            },
        )

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<ApiError> =
        if (exception is ErrorResponse) {
            apiError(exception.statusCode, exception.javaClass.simpleName, exception.body.detail)
        } else {
            logger.error(exception) { "Unexpected error" }
            apiError(HttpStatus.INTERNAL_SERVER_ERROR, "InternalServerError", "Unexpected error")
        }

    private fun apiError(
        status: HttpStatusCode,
        error: String,
        message: String?,
        details: List<String> = emptyList(),
    ) = ResponseEntity.status(status).body(ApiError(error, message, details))
}
