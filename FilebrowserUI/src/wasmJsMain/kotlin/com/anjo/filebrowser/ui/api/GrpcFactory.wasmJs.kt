package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.ui.auth.AccessTokens
import io.ktor.client.HttpClient

actual fun grpcFilebrowserApi(
    backendUrl: String,
    tokens: AccessTokens,
): FilebrowserApi = GrpcWebFilebrowserApi(HttpClient().bearer(tokens), backendUrl)
