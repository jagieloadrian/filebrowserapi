package com.anjo.filebrowser.ui.auth

import com.anjo.filebrowser.ui.model.AuthCallback
import com.anjo.filebrowser.ui.model.SignInAttempt
import com.anjo.filebrowser.ui.model.Tokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

private val EXPIRY_MARGIN = 30.seconds
private val NAME_CLAIMS = listOf("preferred_username", "name")

class AuthSession(
    private val oidc: Oidc?,
    private val browser: AuthBrowser,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
) : AccessTokens {
    private val mutableState = MutableStateFlow(if (oidc == null) AuthState.Off else AuthState.SignedOut())
    val state: StateFlow<AuthState> = mutableState.asStateFlow()
    private val tokens = MutableStateFlow<Tokens?>(null)
    private val renewal = Mutex()
    private var working: Job? = null

    init {
        if (oidc != null) browser.callback()?.let { callback -> work { finish(oidc, callback) } }
    }

    fun signIn() {
        val oidc = oidc ?: return
        work {
            val attempt = SignInAttempt(state = newState(), verifier = newVerifier())
            val discovery = oidc.discovery()
            finish(oidc, browser.authorize(attempt) { oidc.authorizationUrl(discovery, it, attempt) })
        }
    }

    fun cancel() {
        working?.cancel()
        mutableState.value = AuthState.SignedOut()
    }

    fun signOut() {
        val oidc = oidc ?: return
        val ending = oidc.endSessionUrl(tokens.value?.idToken, browser.returnUri)
        tokens.value = null
        if (ending == null) {
            mutableState.value = AuthState.SignedOut()
        } else {
            work {
                browser.open(ending)
                AuthState.SignedOut()
            }
        }
    }

    override suspend fun current(): String? {
        val held = tokens.value ?: return null
        return if (clock.now() < held.expiresAt - EXPIRY_MARGIN) held.access else rejected(held.access)
    }

    override suspend fun rejected(token: String): String? =
        renewal.withLock {
            val held = tokens.value ?: return null
            if (held.access != token) return held.access
            try {
                val fresh = checkNotNull(oidc).refresh(held.refresh ?: throw SignInException(SESSION_ENDED))
                tokens.value = fresh.copy(refresh = fresh.refresh ?: held.refresh, idToken = fresh.idToken ?: held.idToken)
                fresh.access
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                tokens.value = null
                mutableState.value = AuthState.SignedOut(SESSION_ENDED)
                null
            }
        }

    private fun work(block: suspend () -> AuthState) {
        working?.cancel()
        mutableState.value = AuthState.Working
        working =
            scope.launch {
                mutableState.value =
                    try {
                        block()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: SignInException) {
                        AuthState.SignedOut(e.message)
                    } catch (e: Exception) {
                        AuthState.SignedOut(SIGN_IN_UNFINISHED)
                    }
            }
    }

    private suspend fun finish(
        oidc: Oidc,
        callback: AuthCallback,
    ): AuthState {
        val query = callback.query
        val attempt = callback.attempt
        if (ERROR in query || attempt == null || query[STATE] != attempt.state) throw SignInException(SIGN_IN_UNFINISHED)
        val code = query[CODE] ?: throw SignInException(SIGN_IN_UNFINISHED)
        val fresh = oidc.exchange(code, attempt.verifier, callback.redirectUri)
        val user = displayName(fresh.idToken)
        tokens.value = fresh
        return AuthState.SignedIn(user)
    }

    private fun displayName(idToken: String?): String? {
        val payload = idToken?.split('.')?.getOrNull(1) ?: return null
        val claims = Json.parseToJsonElement(BASE64_URL.decode(payload).decodeToString()).jsonObject
        return NAME_CLAIMS.firstNotNullOfOrNull { claims[it]?.jsonPrimitive?.contentOrNull }
    }
}
