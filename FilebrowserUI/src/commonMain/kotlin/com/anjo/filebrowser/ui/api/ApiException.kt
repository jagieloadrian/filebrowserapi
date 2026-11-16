package com.anjo.filebrowser.ui.api

class ApiException(
    val status: ApiStatus,
    serverMessage: String? = null,
    cause: Throwable? = null,
) : Exception(cause) {
    override val message: String =
        serverMessage?.takeIf { it.isNotBlank() && status.showsServerMessage } ?: status.fallback
}
