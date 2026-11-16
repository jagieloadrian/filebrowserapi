package com.anjo.filebrowser.ui.auth

import com.anjo.filebrowser.ui.model.AuthCallback
import com.anjo.filebrowser.ui.model.SignInAttempt

interface AuthBrowser {
    val returnUri: String?

    suspend fun authorize(
        attempt: SignInAttempt,
        url: (redirectUri: String) -> String,
    ): AuthCallback

    fun callback(): AuthCallback?

    suspend fun open(url: String)
}

expect fun authBrowser(): AuthBrowser
