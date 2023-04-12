package com.anjo.filebrowserapi.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalControllerAdvice {


    @ExceptionHandler(value = [CannotRealizeException::class])
    fun handleCannotRealizeException(exception: Exception):ResponseEntity<ErrorResponse> {
        return ResponseEntity(
            ErrorResponse(exception.javaClass.simpleName, exception.message),
            HttpStatus.UNPROCESSABLE_ENTITY
        )
    }
}

data class ErrorResponse(val error: String, val message: String?)
class CannotRealizeException(msg: String) : Exception(msg)
