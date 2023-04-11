package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.constants.Constant.BASE_DIRECTORY
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Service
import java.nio.file.Paths

@Service
@EnableConfigurationProperties(AppConfiguration::class)
class DirectoryManagerService(
    private val properties:AppConfiguration
) {

    fun showBaseDirectory():String {
        return FileSystemResource(Paths.get(generateDefaultPath())).path
    }

    private fun generateDefaultPath():String {
        return properties.baseDiskPath+BASE_DIRECTORY
    }
}