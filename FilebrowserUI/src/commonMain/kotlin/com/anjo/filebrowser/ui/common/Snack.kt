package com.anjo.filebrowser.ui.common

class Snack(
    val message: String,
    val action: String? = null,
    val onAction: () -> Unit = {},
)
