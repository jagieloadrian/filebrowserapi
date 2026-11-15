package com.anjo.filebrowser.ui.auth

interface AccessTokens {
    suspend fun current(): String?

    suspend fun rejected(token: String): String?
}
