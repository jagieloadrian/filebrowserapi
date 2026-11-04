package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.service.DirectoryManager
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("web")
@RequestMapping("/api/v1/directories")
class DirectoryManagerController(
    private val directoryManager: DirectoryManager
) {

    @GetMapping
    fun getDirectoriesTree(@RequestParam("path", required = false) path: String?): ResponseEntity<List<String>> {
        return ResponseEntity.ok(directoryManager.showDirectoriesTree(path))
    }

    @PostMapping
    fun addDirectory(
        @RequestParam("directoryName") directoryName: String,
        @RequestParam("path", required = false) path: String?
    ): ResponseEntity<String> {
        return ResponseEntity(directoryManager.addDirectory(directoryName, path), HttpStatus.CREATED)
    }

    @PostMapping("/recursively")
    fun addDirectoriesRecursively(@RequestParam("path") pathMultiDirectories: String): ResponseEntity<String> {
        return ResponseEntity(
            directoryManager.addDirectoriesRecursively(pathMultiDirectories),
            HttpStatus.CREATED
        )
    }

    @PatchMapping
    fun renameDirectory(
        @RequestParam("sourcePath") path: String,
        @RequestParam("newDirName") newName: String
    ): ResponseEntity<String> {
        return ResponseEntity.ok(directoryManager.renameDirectory(newName, path))
    }

    @DeleteMapping
    fun deleteDirectory(@RequestParam("path") path: String): ResponseEntity<Unit> {
        return ResponseEntity(directoryManager.deleteDirectory(path), HttpStatus.NO_CONTENT)
    }
}