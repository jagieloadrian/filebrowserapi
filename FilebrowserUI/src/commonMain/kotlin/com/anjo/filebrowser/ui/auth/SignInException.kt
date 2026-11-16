package com.anjo.filebrowser.ui.auth

internal const val SIGN_IN_UNFINISHED = "Sign-in did not finish. Try again."
internal const val SIGN_IN_UNREACHABLE = "Could not reach the sign-in service. Check the connection and try again."
internal const val SESSION_ENDED = "Your session has ended. Sign in again to continue."
internal const val BROWSER_UNAVAILABLE = "Could not open the web browser. Set a default browser and try again."

class SignInException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
