package com.anjo.filebrowser.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Instant

private const val HOME = "Home"
private const val STEP = 1024.0
private const val NBSP = '\u00A0'
private val UNITS = listOf("KB", "MB", "GB", "TB")

fun formatSize(bytes: Long): String {
    if (bytes < STEP) return "$bytes${NBSP}B"
    var value = bytes / STEP
    var unit = 0
    while (value >= STEP && unit < UNITS.lastIndex) {
        value /= STEP
        unit++
    }
    val tenths = (value * 10).roundToLong()
    return "${tenths / 10}.${tenths % 10}$NBSP${UNITS[unit]}"
}

expect fun localOffset(instant: Instant): Duration

val LocalTimeOffset = staticCompositionLocalOf<(Instant) -> Duration> { ::localOffset }

fun formatTime(
    instant: Instant,
    offset: Duration = localOffset(instant),
): String = (instant + offset).toString().let { "${it.take(10)}$NBSP${it.substring(11, 16)}" }

fun folderLabel(path: String): String = (listOf(HOME) + path.split('/').filter(String::isNotEmpty)).joinToString(" / ")

internal val Enum<*>.label: String get() = name.lowercase().replaceFirstChar(Char::uppercase)

fun fileType(name: String): String = if (name.lastIndexOf('.') > 0) "${name.substringAfterLast('.').uppercase()} file" else "File"
