package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.constants.Constant.BASE_DIRECTORY
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import java.io.File

@Service
@EnableConfigurationProperties(AppConfiguration::class)
class DirectoryManagerService(
    private val properties: AppConfiguration
) {

    fun showBaseDirectory(): List<String> {
        return File(generateDefaultPath()).walk().map { file -> file.path.substringAfter(generateDefaultPath()) }.drop(1).toList()
    }

    private fun generateDefaultPath(): String {
        return properties.baseDiskPath + BASE_DIRECTORY
    }
}