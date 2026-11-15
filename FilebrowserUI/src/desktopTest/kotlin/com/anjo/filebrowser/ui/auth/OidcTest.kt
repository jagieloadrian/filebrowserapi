package com.anjo.filebrowser.ui.auth

import com.anjo.filebrowser.ui.model.SignInAttempt
import com.anjo.filebrowser.ui.model.Tokens
import com.anjo.filebrowser.ui.model.dto.Discovery
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.io.IOException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val NOW = Instant.parse("2026-11-14T10:00:00Z")
private val FIXED_CLOCK =
    object : Clock {
        override fun now() = NOW
    }

class OidcTest :
    FunSpec({
        test("should read the endpoints from the discovery document of the issuer once") {
            // given
            val provider = FakeIdentityProvider()
            val oidc = provider.oidc()

            // when
            val first = oidc.discovery()
            val second = oidc.discovery()

            // then
            first shouldBe second
            first.tokenEndpoint shouldBe TOKEN_ENDPOINT
            provider.requests.map { "${it.method.value} ${it.url}" } shouldBe
                listOf("${HttpMethod.Get.value} $ISSUER/.well-known/openid-configuration")
        }

        test("should build the authorization URL with PKCE S256 and encoded parameters") {
            // given
            val oidc = FakeIdentityProvider().oidc()
            val discovery = Discovery("$ISSUER/api/oidc/authorization?prompt=login", TOKEN_ENDPOINT)
            val attempt = SignInAttempt(state = "s&t=1", verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")

            // when
            val url = oidc.authorizationUrl(discovery, "http://127.0.0.1:5000/callback", attempt)

            // then
            url shouldBe
                "$ISSUER/api/oidc/authorization?prompt=login&response_type=code&client_id=filebrowser" +
                "&redirect_uri=http%3A%2F%2F127.0.0.1%3A5000%2Fcallback&scope=openid+profile+groups+offline_access" +
                "&state=s%26t%3D1&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&code_challenge_method=S256"
            Url(url).parameters["state"] shouldBe "s&t=1"
            Url(url).parameters["scope"] shouldBe SCOPES
        }

        test("should exchange the code with the verifier at the token endpoint") {
            // given
            val provider = FakeIdentityProvider()

            // when
            val tokens = provider.oidc(FIXED_CLOCK).exchange("the-code", "the-verifier", "http://127.0.0.1:5000/callback")

            // then
            tokens shouldBe Tokens("access-1", "refresh-1", NOW + EXPIRES_IN.seconds, ID_TOKEN)
            provider.requests.last().method shouldBe HttpMethod.Post
            provider.tokenForms
                .single()
                .entries()
                .associate { it.key to it.value.single() } shouldBe
                mapOf(
                    "grant_type" to "authorization_code",
                    "code" to "the-code",
                    "redirect_uri" to "http://127.0.0.1:5000/callback",
                    "client_id" to CLIENT_ID,
                    "code_verifier" to "the-verifier",
                )
        }

        test("should refresh with the refresh token") {
            // given
            val provider = FakeIdentityProvider()

            // when
            val tokens = provider.oidc(FIXED_CLOCK).refresh("refresh-0")

            // then
            tokens shouldBe Tokens("access-1", "refresh-1", NOW + EXPIRES_IN.seconds, null)
            provider.tokenForms
                .single()
                .entries()
                .associate { it.key to it.value.single() } shouldBe
                mapOf("grant_type" to "refresh_token", "refresh_token" to "refresh-0", "client_id" to CLIENT_ID)
        }

        test("should report a token error as an unfinished sign-in") {
            // given
            val provider = FakeIdentityProvider().apply { tokenStatus = HttpStatusCode.BadRequest }

            // when
            val error = shouldThrow<SignInException> { provider.oidc().refresh("refresh-0") }

            // then
            error.message shouldBe SIGN_IN_UNFINISHED
        }

        test("should report an unreachable provider") {
            // given
            val oidc = Oidc(HttpClient(MockEngine { throw IOException("Connection refused") }), ISSUER, CLIENT_ID, SCOPES)

            // when
            val error = shouldThrow<SignInException> { oidc.discovery() }

            // then
            error.message shouldBe SIGN_IN_UNREACHABLE
        }

        test("should end the session only when the provider offers it") {
            // given
            val plain = FakeIdentityProvider().oidc()
            val ending = FakeIdentityProvider(endSession = true).oidc()
            plain.discovery()
            ending.discovery()

            // when & then
            plain.endSessionUrl("id", "https://files.example.test/").shouldBeNull()
            ending.endSessionUrl("id", "https://files.example.test/") shouldBe
                "$END_SESSION_ENDPOINT?client_id=filebrowser&id_token_hint=id" +
                "&post_logout_redirect_uri=https%3A%2F%2Ffiles.example.test%2F"
        }
    })
