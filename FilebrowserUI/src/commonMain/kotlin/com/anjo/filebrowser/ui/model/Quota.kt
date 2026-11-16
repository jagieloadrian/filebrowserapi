package com.anjo.filebrowser.ui.model

import kotlinx.serialization.Serializable

@Serializable
data class Quota(
    val used: Long,
    val limit: Long,
)
