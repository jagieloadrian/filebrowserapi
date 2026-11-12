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

@RestControllerAdvice
@Profile("web")
class RestGlobalControllerAdvice {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(CannotRealizeException::class)
    fun handleCannotRealizeException(exception: CannotRealizeException) =
        apiError(HttpStatus.UNPROCESSABLE_ENTITY, exception.javaClass.simpleName, exception.message)

    @ExceptionHandler(BadInputException::class)
    fun handleBadInputException(exception: BadInputException) =
        apiError(HttpStatus.BAD_REQUEST, exception.javaClass.simpleName, exception.message)

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
    ) = ResponseEntity.status(status).body(ApiError(error, message))
}
