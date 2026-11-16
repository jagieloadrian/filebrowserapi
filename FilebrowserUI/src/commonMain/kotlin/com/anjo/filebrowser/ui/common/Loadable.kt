package com.anjo.filebrowser.ui.common

sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>

    data class Ready<T>(
        val value: T,
    ) : Loadable<T>

    data class Failed(
        val message: String,
    ) : Loadable<Nothing>
}
