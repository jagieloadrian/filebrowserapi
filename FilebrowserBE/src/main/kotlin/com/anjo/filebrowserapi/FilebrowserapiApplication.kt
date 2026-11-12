package com.anjo.filebrowserapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class FilebrowserapiApplication

fun main(args: Array<String>) {
    runApplication<FilebrowserapiApplication>(*args)
}
