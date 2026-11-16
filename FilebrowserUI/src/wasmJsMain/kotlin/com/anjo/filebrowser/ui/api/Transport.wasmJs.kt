package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.ui.auth.AccessTokens
import io.ktor.client.HttpClient

actual fun transportApi(
    backendUrl: String,
    tokens: AccessTokens,
): FilebrowserApi = HttpFilebrowserApi(HttpClient().bearer(tokens), backendUrl)
