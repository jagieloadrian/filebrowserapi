package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.ui.auth.AccessTokens
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

fun HttpClient.bearer(tokens: AccessTokens): HttpClient =
    apply {
        plugin(HttpSend).intercept { request ->
            val token = tokens.current() ?: return@intercept execute(request)
            val call = execute(request.authorized(token))
            if (!call.response.rejectsToken()) return@intercept call
            val renewed = tokens.rejected(token) ?: return@intercept call
            execute(request.authorized(renewed))
        }
    }

private fun HttpRequestBuilder.authorized(token: String) = apply { headers[HttpHeaders.Authorization] = "Bearer $token" }

private fun HttpResponse.rejectsToken() = status == HttpStatusCode.Unauthorized
