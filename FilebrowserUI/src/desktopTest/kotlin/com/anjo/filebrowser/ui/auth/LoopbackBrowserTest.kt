package com.anjo.filebrowser.ui.auth

import com.anjo.filebrowser.ui.model.SignInAttempt
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.ktor.http.Url
import io.ktor.http.encodeURLParameter
import java.io.IOException
import java.net.URI

private val ATTEMPT = SignInAttempt(state = "s1", verifier = "v1")

private fun authorizationUrl(redirectUri: String) = "$ISSUER/authorize?redirect_uri=${redirectUri.encodeURLParameter()}"

class LoopbackBrowserTest :
    FunSpec({
        test("should hand back the callback query and stop the loopback server") {
            // given
            val browser =
                LoopbackBrowser { uri ->
                    val redirect = checkNotNull(Url(uri.toString()).parameters["redirect_uri"])
                    URI("$redirect?code=the-code&state=s1").toURL().readText()
                }

            // when
            val callback = browser.authorize(ATTEMPT, ::authorizationUrl)

            // then
            callback.attempt shouldBe ATTEMPT
            callback.redirectUri shouldMatch Regex("http://127\\.0\\.0\\.1:\\d+/callback")
            callback.query["code"] shouldBe "the-code"
            callback.query["state"] shouldBe "s1"
            shouldThrow<IOException> { URI(callback.redirectUri).toURL().readText() }
        }

        test("should report a missing web browser") {
            // given
            val browser = LoopbackBrowser { throw UnsupportedOperationException("No browser") }

            // when
            val error = shouldThrow<SignInException> { browser.authorize(ATTEMPT, ::authorizationUrl) }

            // then
            error.message shouldBe BROWSER_UNAVAILABLE
        }
    })
