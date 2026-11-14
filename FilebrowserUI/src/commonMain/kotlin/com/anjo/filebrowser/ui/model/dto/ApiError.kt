package com.anjo.filebrowser.ui.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val message: String? = null,
)
