package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.model.FileVersion
import com.anjo.filebrowserapi.service.VersionManager
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("web")
@RequestMapping("/api/v1/versions")
class VersionController(
    private val versionManager: VersionManager,
) {
    @GetMapping
    fun listVersions(
        @RequestParam("path") path: String,
    ): List<FileVersion> = versionManager.list(path)

    @PostMapping("/{id}/restore")
    fun restore(
        @PathVariable id: String,
    ): String = versionManager.restore(id)
}
