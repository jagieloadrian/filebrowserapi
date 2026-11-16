package com.anjo.filebrowser.ui.auth

import com.anjo.filebrowser.ui.model.SignInAttempt
import com.anjo.filebrowser.ui.model.Tokens
import com.anjo.filebrowser.ui.model.dto.Discovery
import com.anjo.filebrowser.ui.model.dto.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal const val CODE = "code"
internal const val STATE = "state"
internal const val ERROR = "error"
private const val DISCOVERY_PATH = "/.well-known/openid-configuration"
private const val CLIENT_ID = "client_id"
private const val REDIRECT_URI = "redirect_uri"
private const val GRANT_TYPE = "grant_type"
private const val S256 = "S256"

class Oidc(
    private val client: HttpClient,
    issuer: String,
    private val clientId: String,
    private val scopes: String,
    private val clock: Clock = Clock.System,
) {
    private val discoveryUrl = issuer.trimEnd('/') + DISCOVERY_PATH
    private val json = Json { ignoreUnknownKeys = true }
    private var known: Discovery? = null

    suspend fun discovery(): Discovery = known ?: json.decodeFromString<Discovery>(fetch { client.get(discoveryUrl) }).also { known = it }

    fun authorizationUrl(
        discovery: Discovery,
        redirectUri: String,
        attempt: SignInAttempt,
    ): String =
        URLBuilder(discovery.authorizationEndpoint)
            .apply {
                parameters.append("response_type", CODE)
                parameters.append(CLIENT_ID, clientId)
                parameters.append(REDIRECT_URI, redirectUri)
                parameters.append("scope", scopes)
                parameters.append(STATE, attempt.state)
                parameters.append("code_challenge", codeChallenge(attempt.verifier))
                parameters.append("code_challenge_method", S256)
            }.buildString()

    suspend fun exchange(
        code: String,
        verifier: String,
        redirectUri: String,
    ): Tokens =
        token(
            parameters {
                append(GRANT_TYPE, "authorization_code")
                append(CODE, code)
                append(REDIRECT_URI, redirectUri)
                append(CLIENT_ID, clientId)
                append("code_verifier", verifier)
            },
        )

    suspend fun refresh(refreshToken: String): Tokens =
        token(
            parameters {
                append(GRANT_TYPE, "refresh_token")
                append("refresh_token", refreshToken)
                append(CLIENT_ID, clientId)
            },
        )

    fun endSessionUrl(
        idToken: String?,
        returnUri: String?,
    ): String? =
        known?.endSessionEndpoint?.let { endpoint ->
            URLBuilder(endpoint)
                .apply {
                    parameters.append(CLIENT_ID, clientId)
                    idToken?.let { parameters.append("id_token_hint", it) }
                    returnUri?.let { parameters.append("post_logout_redirect_uri", it) }
                }.buildString()
        }

    private suspend fun token(form: Parameters): Tokens {
        val endpoint = discovery().tokenEndpoint
        val reply = json.decodeFromString<TokenResponse>(fetch { client.submitForm(endpoint, form) })
        val expiresAt = reply.expiresIn?.let { clock.now() + it.seconds } ?: Instant.DISTANT_FUTURE
        return Tokens(reply.accessToken, reply.refreshToken, expiresAt, reply.idToken)
    }

    private suspend fun fetch(send: suspend () -> HttpResponse): String {
        val response =
            try {
                send()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw SignInException(SIGN_IN_UNREACHABLE, e)
            }
        if (!response.status.isSuccess()) throw SignInException(SIGN_IN_UNFINISHED)
        return response.bodyAsText()
    }
}
