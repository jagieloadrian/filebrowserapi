package com.anjo.filebrowser.ui.api

enum class ApiStatus(
    val fallback: String,
    val showsServerMessage: Boolean = true,
) {
    INVALID("The server did not accept the request. Check the name and try again."),
    UNAUTHORIZED("The server asks for sign-in, which this app does not support yet.", showsServerMessage = false),
    NOT_FOUND("This item no longer exists. Refresh and try again."),
    CONFLICT("An item with this name already exists."),
    TOO_LARGE("The file is larger than the upload limit."),
    NO_SPACE("Not enough storage left. Empty the trash or delete files, then try again."),
    OFFLINE("Could not reach the server. Check the connection and try again.", showsServerMessage = false),
    FAILED("The server could not finish the request. Try again later.", showsServerMessage = false),
}
