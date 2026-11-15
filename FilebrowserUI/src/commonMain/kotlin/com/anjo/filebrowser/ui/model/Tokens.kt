package com.anjo.filebrowser.ui.model

import kotlin.time.Instant

data class Tokens(
    val access: String,
    val refresh: String?,
    val expiresAt: Instant,
    val idToken: String?,
)
