package com.anjo.filebrowserapi.utils

internal fun formatResponsePath(
    fullPath: String,
    defaultPath: String,
): String = fullPath.substringAfter(defaultPath)
