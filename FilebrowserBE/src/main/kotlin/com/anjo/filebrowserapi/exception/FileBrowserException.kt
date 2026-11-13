package com.anjo.filebrowserapi.exception

sealed class FileBrowserException(
    message: String,
) : Exception(message)

class BadInputException(
    message: String,
) : FileBrowserException(message)

class NotFoundException(
    message: String,
) : FileBrowserException(message)

class AlreadyExistsException(
    message: String,
) : FileBrowserException(message)

class NotEmptyException(
    message: String,
) : FileBrowserException(message)

class QuotaExceededException(
    message: String,
) : FileBrowserException(message)
