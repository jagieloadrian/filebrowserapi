package com.anjo.filebrowser.ui.common

import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant

actual fun localOffset(instant: Instant): Duration =
    ZoneId
        .systemDefault()
        .rules
        .getOffset(instant.toJavaInstant())
        .totalSeconds.seconds
