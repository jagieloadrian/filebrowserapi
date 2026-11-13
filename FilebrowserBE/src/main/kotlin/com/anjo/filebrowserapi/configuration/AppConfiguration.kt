package com.anjo.filebrowserapi.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize

@ConfigurationProperties("application")
data class AppConfiguration(
    val path: String = System.getProperty("user.dir"),
    val maxUploadSize: DataSize = DataSize.ofGigabytes(1),
    val quota: DataSize = DataSize.ofGigabytes(5),
    val minFreeSpace: DataSize = DataSize.ofGigabytes(2),
) {
    init {
        require(path.isNotBlank()) { "application.path must not be blank" }
    }
}
