@file:OptIn(ExperimentalWasmJsInterop::class)

package com.anjo.filebrowser.ui.common

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private fun minutesBehindUtc(epochMillis: Double): Double = js("new Date(epochMillis).getTimezoneOffset()")

actual fun localOffset(instant: Instant): Duration = -minutesBehindUtc(instant.toEpochMilliseconds().toDouble()).minutes
