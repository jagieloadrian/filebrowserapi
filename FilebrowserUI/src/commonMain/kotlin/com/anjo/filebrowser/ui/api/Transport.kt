package com.anjo.filebrowser.ui.api

import com.anjo.filebrowser.ui.auth.AccessTokens

expect fun transportApi(
    backendUrl: String,
    tokens: AccessTokens,
): FilebrowserApi
