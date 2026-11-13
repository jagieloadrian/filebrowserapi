package com.anjo.filebrowserapi.repository

internal const val UNDER = "owner = :owner and (path = :path or path like :prefix escape '\\')"

internal fun likePrefix(path: String): String =
    path
        .trimEnd('/')
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_") + "/%"
