package com.anjo.filebrowserapi.exception

import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Profile("web")
class RestGlobalControllerAdvice {
    @ExceptionHandler(value = [CannotRealizeException::class])
    fun handleCannotRealizeException(exception: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(exception.javaClass.simpleName, exception.message),
            HttpStatus.UNPROCESSABLE_ENTITY,
        )

    @ExceptionHandler(value = [BadInputException::class])
    fun handleBadInputException(exception: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(exception.javaClass.simpleName, exception.message),
            HttpStatus.BAD_REQUEST,
        )
}

data class ErrorResponse(
    val error: String,
    val message: String?,
)
