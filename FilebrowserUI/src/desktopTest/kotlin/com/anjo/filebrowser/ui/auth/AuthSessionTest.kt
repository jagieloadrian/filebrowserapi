package com.anjo.filebrowser.ui.auth

import com.anjo.filebrowser.ui.model.AuthCallback
import com.anjo.filebrowser.ui.model.SignInAttempt
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.parametersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val REDIRECT = "http://127.0.0.1:5000/callback"

private class FakeBrowser(
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

private class SteppedClock : Clock {
    var time = Instant.parse("2026-11-14T10:00:00Z")

    override fun now() = time
}

private data class AnswerCase(
    val name: String,
    val answer: (SignInAttempt) -> Parameters,
)

private suspend fun AuthSession.settled(): AuthState = withTimeout(5.seconds) { state.first { it != AuthState.Working } }

class AuthSessionTest :
    FunSpec({
        lateinit var scope: CoroutineScope
        lateinit var provider: FakeIdentityProvider
        lateinit var clock: SteppedClock

        beforeTest {
            scope = CoroutineScope(SupervisorJob())
            provider = FakeIdentityProvider(endSession = true)
            clock = SteppedClock()
        }

        afterTest { scope.cancel() }

        fun session(browser: AuthBrowser = FakeBrowser()) = AuthSession(provider.oidc(clock), browser, scope, clock)

        suspend fun signedIn(browser: FakeBrowser = FakeBrowser()) =
            session(browser).apply {
                signIn()
                settled() shouldBe AuthState.SignedIn("alice")
            }

        test("should stay off and hand out no token when the issuer is empty") {
            // given
            val browser = FakeBrowser()
            val session = AuthSession(null, browser, scope)

            // when
            session.signIn()

            // then
            session.state.value shouldBe AuthState.Off
            session.current().shouldBeNull()
            browser.authorizationUrl.shouldBeNull()
        }

        test("should start signed out when sign-in is on") {
            // when & then
            session().state.value shouldBe AuthState.SignedOut()
        }

        test("should sign in with the code and the verifier that match the authorization request") {
            // given
            val browser = FakeBrowser()

            // when
            val session = signedIn(browser)

            // then
            val request = Url(checkNotNull(browser.authorizationUrl)).parameters
            val form = provider.tokenForms.single()
            form["code"] shouldBe "the-code"
            codeChallenge(checkNotNull(form["code_verifier"])) shouldBe request["code_challenge"]
            form["redirect_uri"] shouldBe REDIRECT
            session.current() shouldBe "access-1"
        }

        test("should show the loading state while the browser is open and sign out on cancel") {
            // given
            val session = session(FakeBrowser(answer = { awaitCancellation() }))

            // when
            session.signIn()
            withTimeout(5.seconds) { session.state.first { it == AuthState.Working } }
            session.cancel()

            // then
            session.state.value shouldBe AuthState.SignedOut()
            provider.tokenForms.shouldBeEmpty()
        }

        context("rejected answers") {
            withTests(
                nameFn = { "should not exchange the code on ${it.name}" },
                AnswerCase("a state mismatch") { parametersOf(CODE to listOf("the-code"), STATE to listOf("forged")) },
                AnswerCase("a provider error") { parametersOf(ERROR to listOf("access_denied"), STATE to listOf(it.state)) },
                AnswerCase("a missing code") { parametersOf(STATE to listOf(it.state)) },
            ) { case ->
                // given
                val session = session(FakeBrowser(answer = case.answer))

                // when
                session.signIn()

                // then
                session.settled() shouldBe AuthState.SignedOut(SIGN_IN_UNFINISHED)
                provider.tokenForms.shouldBeEmpty()
                session.current().shouldBeNull()
            }
        }

        test("should finish a sign-in that returns to the app") {
            // given
            val attempt = SignInAttempt(state = "s1", verifier = "v1")
            val returned = AuthCallback(attempt, REDIRECT, parametersOf(CODE to listOf("the-code"), STATE to listOf("s1")))

            // when
            val session = session(FakeBrowser(returned = returned))

            // then
            session.settled() shouldBe AuthState.SignedIn("alice")
            provider.tokenForms.single()["code_verifier"] shouldBe "v1"
        }

        test("should reject a returning sign-in without a stored attempt") {
            // given
            val returned = AuthCallback(null, REDIRECT, parametersOf(CODE to listOf("the-code"), STATE to listOf("s1")))

            // when
            val session = session(FakeBrowser(returned = returned))

            // then
            session.settled() shouldBe AuthState.SignedOut(SIGN_IN_UNFINISHED)
            provider.tokenForms.shouldBeEmpty()
        }

        test("should refresh the access token shortly before it expires") {
            // given
            val session = signedIn()
            clock.time += (EXPIRES_IN - 10).seconds

            // when
            val token = session.current()

            // then
            token shouldBe "access-2"
            provider.tokenForms.last()["grant_type"] shouldBe "refresh_token"
            provider.tokenForms.last()["refresh_token"] shouldBe "refresh-1"
            session.state.value shouldBe AuthState.SignedIn("alice")
        }

        test("should refresh once for every caller of a rejected token") {
            // given
            val session = signedIn()

            // when
            val first = session.rejected("access-1")
            val second = session.rejected("access-1")

            // then
            first shouldBe "access-2"
            second shouldBe "access-2"
            provider.tokenForms.size shouldBe 2
        }

        test("should sign out when the refresh fails") {
            // given
            val session = signedIn()
            provider.tokenStatus = HttpStatusCode.BadRequest

            // when
            val token = session.rejected("access-1")

            // then
            token.shouldBeNull()
            session.state.value shouldBe AuthState.SignedOut(SESSION_ENDED)
            session.current().shouldBeNull()
        }

        test("should drop the tokens and end the provider session on sign out") {
            // given
            val browser = FakeBrowser()
            val session = signedIn(browser)

            // when
            session.signOut()

            // then
            session.settled() shouldBe AuthState.SignedOut()
            session.current().shouldBeNull()
            browser.opened shouldBe listOf("$END_SESSION_ENDPOINT?client_id=filebrowser&id_token_hint=$ID_TOKEN")
        }

        test("should sign out locally when the provider has no end session endpoint") {
            // given
            provider = FakeIdentityProvider()
            val browser = FakeBrowser()
            val session = signedIn(browser)

            // when
            session.signOut()

            // then
            session.state.value shouldBe AuthState.SignedOut()
            browser.opened.shouldBeEmpty()
        }
    })
