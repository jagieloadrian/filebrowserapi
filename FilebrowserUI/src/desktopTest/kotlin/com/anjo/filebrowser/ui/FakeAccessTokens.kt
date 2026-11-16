package com.anjo.filebrowser.ui

import com.anjo.filebrowser.ui.auth.AccessTokens

class FakeAccessTokens(
    var token: String? = "t1",
    var renewed: String? = "t2",
) : AccessTokens {
    val rejectedTokens = mutableListOf<String>()

    override suspend fun current(): String? = token

    override suspend fun rejected(token: String): String? {
        rejectedTokens += token
        return renewed?.also { this.token = it }
    }
}
