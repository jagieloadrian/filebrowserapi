package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.model.TrashItem
import com.anjo.filebrowserapi.service.TrashManager
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("web")
@RequestMapping("/api/v1/trash")
class TrashController(
    private val trashManager: TrashManager,
) {
    @GetMapping
    fun listTrash(): List<TrashItem> = trashManager.list()

    @PostMapping("/{id}/restore")
    fun restore(
        @PathVariable id: String,
    ): String = trashManager.restore(id)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun purge(
        @PathVariable id: String,
    ) = trashManager.purge(id)

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun emptyTrash() = trashManager.empty()
}
