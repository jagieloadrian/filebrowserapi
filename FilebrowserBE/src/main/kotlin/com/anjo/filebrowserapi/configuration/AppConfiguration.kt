package com.anjo.filebrowserapi.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize

@ConfigurationProperties("application")
data class AppConfiguration(
    val path: String = System.getProperty("user.dir"),
    val maxUploadSize: DataSize = DataSize.ofGigabytes(1),
) {
    init {
        require(path.isNotBlank()) { "application.path must not be blank" }
    }
}
