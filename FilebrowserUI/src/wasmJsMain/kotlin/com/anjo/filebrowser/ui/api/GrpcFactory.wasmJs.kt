package com.anjo.filebrowser.ui.api

import io.ktor.client.HttpClient

actual fun grpcFilebrowserApi(backendUrl: String): FilebrowserApi = GrpcWebFilebrowserApi(HttpClient(), backendUrl)
