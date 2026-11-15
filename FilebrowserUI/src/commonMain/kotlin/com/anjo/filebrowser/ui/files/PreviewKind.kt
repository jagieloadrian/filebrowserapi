package com.anjo.filebrowser.ui.files

import com.anjo.filebrowser.ui.model.Entry

private const val MIB = 1024L * 1024

enum class PreviewKind(
    val maxBytes: Long,
    val partial: Boolean,
) {
    TEXT(MIB, partial = true),
    IMAGE(20 * MIB, partial = false),
}

private val TEXT_EXTENSIONS =
    setOf(
        "txt",
        "md",
        "log",
        "csv",
        "tsv",
        "json",
        "yaml",
        "yml",
        "xml",
        "toml",
        "ini",
        "conf",
        "properties",
        "html",
        "css",
        "sql",
        "sh",
        "kt",
        "kts",
        "java",
        "js",
        "ts",
        "py",
        "go",
        "rs",
        "c",
        "h",
        "cpp",
    )
private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")
private val TEXT_TYPES = setOf("application/json", "application/xml", "application/javascript")
private val IMAGE_TYPES = setOf("image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp")

fun previewKind(entry: Entry): PreviewKind? {
    val extension = entry.name.substringAfterLast('.', "").lowercase()
    val type =
        entry.contentType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            .orEmpty()
    return when {
        entry.directory -> null
        extension in TEXT_EXTENSIONS -> PreviewKind.TEXT
        extension in IMAGE_EXTENSIONS -> PreviewKind.IMAGE
        type.startsWith("text/") || type in TEXT_TYPES -> PreviewKind.TEXT
        type in IMAGE_TYPES -> PreviewKind.IMAGE
        else -> null
    }
}
