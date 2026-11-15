@file:OptIn(ExperimentalWasmJsInterop::class)

package com.anjo.filebrowser.ui.auth

import com.anjo.filebrowser.ui.model.AuthCallback
import com.anjo.filebrowser.ui.model.SignInAttempt
import io.ktor.http.parseQueryString
import kotlinx.browser.window
import kotlinx.coroutines.awaitCancellation

private const val STATE_KEY = "filebrowser.signIn.state"
private const val VERIFIER_KEY = "filebrowser.signIn.verifier"

class RedirectBrowser : AuthBrowser {
    override val returnUri: String get() = "${window.location.origin}/"

    override suspend fun authorize(
        attempt: SignInAttempt,
        url: (redirectUri: String) -> String,
    ): AuthCallback {
        window.sessionStorage.setItem(STATE_KEY, attempt.state)
        window.sessionStorage.setItem(VERIFIER_KEY, attempt.verifier)
        window.location.assign(url(returnUri))
        awaitCancellation()
    }

    override fun callback(): AuthCallback? {
        val query = parseQueryString(window.location.search.removePrefix("?"))
        if (CODE !in query && ERROR !in query) return null
        val storage = window.sessionStorage
        val state = storage.getItem(STATE_KEY)
        val verifier = storage.getItem(VERIFIER_KEY)
        storage.removeItem(STATE_KEY)
        storage.removeItem(VERIFIER_KEY)
        window.history.replaceState(null, "", window.location.pathname)
        val attempt = if (state != null && verifier != null) SignInAttempt(state, verifier) else null
        return AuthCallback(attempt, returnUri, query)
    }

    override suspend fun open(url: String) = window.location.assign(url)
}

actual fun authBrowser(): AuthBrowser = RedirectBrowser()
