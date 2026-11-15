package com.anjo.filebrowser.ui.common

import kotlin.math.roundToLong
import kotlin.time.Instant

private const val HOME = "Home"
private const val STEP = 1024.0
private val UNITS = listOf("KB", "MB", "GB", "TB")

fun formatSize(bytes: Long): String {
    if (bytes < STEP) return "$bytes B"
    var value = bytes / STEP
    var unit = 0
    while (value >= STEP && unit < UNITS.lastIndex) {
        value /= STEP
        unit++
    }
    val tenths = (value * 10).roundToLong()
    return "${tenths / 10}.${tenths % 10} ${UNITS[unit]}"
}

fun formatTime(instant: Instant): String = instant.toString().let { "${it.take(10)} ${it.substring(11, 16)} UTC" }

fun folderLabel(path: String): String = path.ifEmpty { HOME }

fun fileType(name: String): String = if (name.lastIndexOf('.') > 0) "${name.substringAfterLast('.').uppercase()} file" else "File"
