package com.anjo.filebrowserapi.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("application")
class AppConfiguration(
        val baseDiskPath: String = System.getProperty("user.dir"))