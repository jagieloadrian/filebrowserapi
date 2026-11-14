package com.anjo.filebrowser.ui.api

internal const val GRPC_OK = 0
private const val INVALID_ARGUMENT = 3
private const val DEADLINE_EXCEEDED = 4
private const val NOT_FOUND = 5
private const val ALREADY_EXISTS = 6
private const val PERMISSION_DENIED = 7
private const val RESOURCE_EXHAUSTED = 8
private const val FAILED_PRECONDITION = 9
private const val UNAVAILABLE = 14
private const val UNAUTHENTICATED = 16

internal fun apiStatusOfGrpc(code: Int): ApiStatus =
    when (code) {
        INVALID_ARGUMENT -> ApiStatus.INVALID
        UNAUTHENTICATED, PERMISSION_DENIED -> ApiStatus.UNAUTHORIZED
        NOT_FOUND -> ApiStatus.NOT_FOUND
        ALREADY_EXISTS, FAILED_PRECONDITION -> ApiStatus.CONFLICT
        RESOURCE_EXHAUSTED -> ApiStatus.NO_SPACE
        UNAVAILABLE, DEADLINE_EXCEEDED -> ApiStatus.OFFLINE
        else -> ApiStatus.FAILED
    }
