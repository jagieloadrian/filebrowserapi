package com.anjo.filebrowserapi.configuration

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("application")
class AppConfiguration(
    private var path: String?,
) {
    private val logger = KotlinLogging.logger {}

    fun getPath() = path

    @PostConstruct
    private fun validateBasePath() {
        if (path.isNullOrBlank() || path.isNullOrEmpty()) {
            path = System.getProperty("user.dir")
            logger.info { "Property application.path was blank. Defaulting to: $path" }
        }
    }
}
