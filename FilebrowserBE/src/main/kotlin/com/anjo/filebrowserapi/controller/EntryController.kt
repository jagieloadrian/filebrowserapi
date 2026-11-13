package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.model.Entry
import com.anjo.filebrowserapi.service.EntryManager
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("web")
@RequestMapping("/api/v1/entries")
class EntryController(
    private val entryManager: EntryManager,
) {
    @GetMapping
    fun listEntries(
        @RequestParam("path", required = false) path: String?,
        @RequestParam("name", required = false) name: String?,
        @RequestParam("extension", required = false) extension: String?,
    ): List<Entry> = entryManager.listEntries(path, name, extension)
}
