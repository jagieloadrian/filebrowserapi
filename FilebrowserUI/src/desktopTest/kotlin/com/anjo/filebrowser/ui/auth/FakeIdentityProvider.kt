package com.anjo.filebrowser.ui.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import kotlin.time.Clock

const val ISSUER = "https://auth.example.test"
const val CLIENT_ID = "filebrowser"
const val SCOPES = "openid profile groups offline_access"
const val TOKEN_ENDPOINT = "$ISSUER/api/oidc/token"
const val END_SESSION_ENDPOINT = "$ISSUER/logout"
const val EXPIRES_IN = 3600L
val ID_TOKEN = idToken("alice")

fun idToken(user: String) =
    "eyJhbGciOiJub25lIn0.${BASE64_URL.encode("""{"sub":"42","preferred_username":"$user","name":"Alice"}""".encodeToByteArray())}.sig"

class FakeIdentityProvider(
    endSession: Boolean = false,
    user: String = "alice",
) {
    val requests = mutableListOf<HttpRequestData>()
    var tokenStatus = HttpStatusCode.OK
    private var issued = 0
    private val discovery =
        """{"issuer":"$ISSUER","authorization_endpoint":"$ISSUER/api/oidc/authorization","token_endpoint":"$TOKEN_ENDPOINT"""" +
            (if (endSession) ""","end_session_endpoint":"$END_SESSION_ENDPOINT"}""" else "}")
    val client =
        HttpClient(
            MockEngine { request ->
                requests += request
                val json = headersOf(HttpHeaders.ContentType, "application/json")
                if (request.url.toString() == TOKEN_ENDPOINT) {
                    issued++
                    val idToken = if (request.form()["grant_type"] == "authorization_code") ""","id_token":"${idToken(user)}"""" else ""
                    respond(
                        """{"access_token":"access-$issued","refresh_token":"refresh-$issued","expires_in":$EXPIRES_IN$idToken}""",
                        tokenStatus,
                        json,
                    )
                } else {
                    respond(discovery, HttpStatusCode.OK, json)
                }
            },
        )

    val tokenForms get() = requests.filter { it.url.toString() == TOKEN_ENDPOINT }.map { it.form() }

    fun oidc(clock: Clock = Clock.System) = Oidc(client, "$ISSUER/", CLIENT_ID, SCOPES, clock)

    private fun HttpRequestData.form(): Parameters = (body as FormDataContent).formData
}
