package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.model.Entry
import com.anjo.filebrowserapi.service.EntryManager
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
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

    @PostMapping("/copy")
    @ResponseStatus(HttpStatus.CREATED)
    fun copyEntry(
        @RequestParam("source") source: String,
        @RequestParam("target") target: String,
    ): String = entryManager.copyEntry(source, target)

    @PostMapping("/move")
    fun moveEntry(
        @RequestParam("source") source: String,
        @RequestParam("target") target: String,
    ): String = entryManager.moveEntry(source, target)
}
