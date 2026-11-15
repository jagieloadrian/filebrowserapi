package com.anjo.filebrowser.ui.auth

import com.anjo.filebrowser.ui.model.AuthCallback
import com.anjo.filebrowser.ui.model.SignInAttempt
import io.ktor.http.Parameters
import io.ktor.http.parametersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

internal const val REDIRECT = "http://127.0.0.1:5000/callback"

internal class FakeBrowser(
    private val returned: AuthCallback? = null,
    private val answer: suspend (SignInAttempt) -> Parameters = { parametersOf(CODE to listOf("the-code"), STATE to listOf(it.state)) },
) : AuthBrowser {
    val opened = mutableListOf<String>()
    var authorizationUrl: String? = null
    override val returnUri: String? = null

    override suspend fun authorize(
        attempt: SignInAttempt,
        url: (redirectUri: String) -> String,
    ): AuthCallback {
        authorizationUrl = url(REDIRECT)
        return AuthCallback(attempt, REDIRECT, answer(attempt))
    }

    override fun callback(): AuthCallback? = returned

    override suspend fun open(url: String) {
        opened += url
    }
}

internal suspend fun AuthSession.settled(): AuthState = withTimeout(5.seconds) { state.first { it != AuthState.Working } }
