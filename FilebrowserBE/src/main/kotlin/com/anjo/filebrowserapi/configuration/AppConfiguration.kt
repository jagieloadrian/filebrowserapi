package com.anjo.filebrowserapi.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("application")
data class AppConfiguration(
    val path: String = System.getProperty("user.dir"),
) {
    init {
        require(path.isNotBlank()) { "application.path must not be blank" }
    }
}
