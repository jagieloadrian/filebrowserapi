package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.ui.FakeAccessTokens
import com.anjo.filebrowser.ui.auth.AuthSession
import com.anjo.filebrowser.ui.auth.LoopbackBrowser
import com.anjo.filebrowser.ui.model.PickedFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.anjo.filebrowser.Quota as QuotaMessage

private const val BASE = "http://localhost:8080"
private const val QUOTA_JSON = """{"used":5,"limit":10}"""

private class TokenServer(
    val tokens: FakeAccessTokens = FakeAccessTokens(),
    private val answer: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) {
    val requests = mutableListOf<HttpRequestData>()
    val client =
        HttpClient(
            MockEngine { request ->
                requests += request
                answer(request)
            },
        ).bearer(tokens)
    val authorizations get() = requests.map { it.headers[HttpHeaders.Authorization] }
}

private fun MockRequestHandleScope.json(status: HttpStatusCode = HttpStatusCode.OK) =
    respond(QUOTA_JSON, status, headersOf(HttpHeaders.ContentType, "application/json"))

private fun MockRequestHandleScope.grpcWebQuota() = respond(grpcWebFrame(QuotaMessage(used = 5, limit = 10).encode()) + OK_TRAILER)

private data class TransportCase(
    val name: String,
    val answer: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    val call: suspend (HttpClient) -> Unit,
)

class BearerTest :
    FunSpec({
        context("attaching the token") {
            withTests(
                nameFn = { "should send the bearer token over ${it.name}" },
                TransportCase("HTTP", { json() }, { HttpFilebrowserApi(it, BASE).quota() }),
                TransportCase("gRPC-Web", { grpcWebQuota() }, { GrpcWebFilebrowserApi(it, BASE).quota() }),
                TransportCase("the REST upload of gRPC-Web", { json() }, {
                    GrpcWebFilebrowserApi(it, BASE).upload("/", PickedFile("a.txt", byteArrayOf(1)), overwrite = false)
                }),
            ) { case ->
                // given
                val server = TokenServer(answer = case.answer)

                // when
                case.call(server.client)

                // then
                server.authorizations shouldBe listOf("Bearer t1")
            }
        }

        test("should send no token when sign-in is off") {
            // given
            val session = AuthSession(null, LoopbackBrowser(), CoroutineScope(SupervisorJob()))
            val requests = mutableListOf<HttpRequestData>()
            val client =
                HttpClient(
                    MockEngine {
                        requests += it
                        json()
                    },
                ).bearer(session)

            // when
            HttpFilebrowserApi(client, BASE).quota()

            // then
            requests.single().headers[HttpHeaders.Authorization] shouldBe null
        }

        test("should retry once with a renewed token after a 401") {
            // given
            val server =
                TokenServer {
                    if (it.headers[HttpHeaders.Authorization] ==
                        "Bearer t1"
                    ) {
                        json(HttpStatusCode.Unauthorized)
                    } else {
                        json()
                    }
                }

            // when
            HttpFilebrowserApi(server.client, BASE).quota()

            // then
            server.authorizations shouldBe listOf("Bearer t1", "Bearer t2")
            server.tokens.rejectedTokens shouldBe listOf("t1")
        }

        test("should retry once with a renewed token after a trailers-only UNAUTHENTICATED") {
            // given
            val server =
                TokenServer {
                    if (it.headers[HttpHeaders.Authorization] == "Bearer t1") {
                        respond(byteArrayOf(), HttpStatusCode.OK, headersOf(GRPC_STATUS, "$UNAUTHENTICATED"))
                    } else {
                        grpcWebQuota()
                    }
                }

            // when
            GrpcWebFilebrowserApi(server.client, BASE).quota()

            // then
            server.authorizations shouldBe listOf("Bearer t1", "Bearer t2")
        }

        test("should report signed out when no renewed token is available") {
            // given
            val server = TokenServer(FakeAccessTokens(renewed = null)) { json(HttpStatusCode.Unauthorized) }

            // when
            val error = shouldThrow<ApiException> { HttpFilebrowserApi(server.client, BASE).quota() }

            // then
            error.status shouldBe ApiStatus.SIGNED_OUT
            server.authorizations shouldBe listOf("Bearer t1")
        }

        test("should not retry a request that is forbidden") {
            // given
            val server = TokenServer { json(HttpStatusCode.Forbidden) }

            // when
            val error = shouldThrow<ApiException> { HttpFilebrowserApi(server.client, BASE).quota() }

            // then
            error.status shouldBe ApiStatus.FORBIDDEN
            error.message shouldBe ApiStatus.FORBIDDEN.fallback
            server.tokens.rejectedTokens.shouldBeEmpty()
        }
    })
