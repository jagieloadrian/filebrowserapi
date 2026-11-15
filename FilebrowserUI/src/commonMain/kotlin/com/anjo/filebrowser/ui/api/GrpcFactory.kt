package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.ui.auth.AccessTokens

expect fun grpcFilebrowserApi(
    backendUrl: String,
    tokens: AccessTokens,
): FilebrowserApi
