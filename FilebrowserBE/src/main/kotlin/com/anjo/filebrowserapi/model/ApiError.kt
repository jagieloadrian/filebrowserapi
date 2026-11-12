package com.anjo.filebrowserapi.model

data class ApiError(
    val error: String,
    val message: String?,
    val details: List<String> = emptyList(),
)
