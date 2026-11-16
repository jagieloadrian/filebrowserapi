package com.anjo.filebrowser.ui.model

import io.ktor.http.Parameters

data class AuthCallback(
    val attempt: SignInAttempt?,
    val redirectUri: String,
    val query: Parameters,
)
