package com.anjo.filebrowser.ui.validation

fun nameError(name: String): String? =
    when {
        name.isBlank() -> "Enter a name."
        '/' in name -> "Remove the / character. Names cannot contain it."
        else -> null
    }
