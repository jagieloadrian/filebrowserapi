package com.anjo.filebrowser.ui.auth

sealed interface AuthState {
    data object Off : AuthState

    data object Working : AuthState

    data class SignedOut(
        val error: String? = null,
    ) : AuthState

    data class SignedIn(
        val user: String?,
    ) : AuthState
}
