package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.model.CreatedShare
import com.anjo.filebrowserapi.model.Share
import com.anjo.filebrowserapi.service.ShareManager
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("web")
class ShareController(
    private val shareManager: ShareManager,
) {
    @PostMapping(SHARES)
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestParam("path") path: String,
        @RequestParam("days", required = false) days: Long?,
    ): CreatedShare = shareManager.create(path, days)

    @GetMapping(SHARES)
    fun list(): List<Share> = shareManager.list()

    @DeleteMapping("$SHARES/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(
        @PathVariable id: String,
    ) = shareManager.revoke(id)

    @GetMapping("/share/{token}")
    fun open(
        @PathVariable token: String,
    ): ResponseEntity<Resource> = shareManager.open(token).download()

    private companion object {
        const val SHARES = "/api/v1/shares"
    }
}
