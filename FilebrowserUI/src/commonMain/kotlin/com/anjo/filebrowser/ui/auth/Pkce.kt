package com.anjo.filebrowser.ui.auth

import io.ktor.util.sha256
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid

internal val BASE64_URL = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

internal fun newState(): String = Uuid.random().toHexString()

internal fun newVerifier(): String = Uuid.random().toHexString() + Uuid.random().toHexString()

internal fun codeChallenge(verifier: String): String = BASE64_URL.encode(sha256(verifier.encodeToByteArray()))
