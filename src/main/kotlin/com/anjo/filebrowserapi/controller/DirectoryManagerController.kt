package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.service.DirectoryManagerService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/directories")
class DirectoryManagerController(
    private val directoryManagerService: DirectoryManagerService
) {

    @GetMapping
    fun getDirectoriesTree(@RequestParam("path", required = false) path: String?): ResponseEntity<List<String>> {
        return ResponseEntity.ok(directoryManagerService.showDirectoriesTree(path))
    }

    @PostMapping
    fun addDirectory(
        @RequestParam("directoryName") directoryName: String,
        @RequestParam("path", required = false) path: String?
    ): ResponseEntity<String> {
        return ResponseEntity(directoryManagerService.addDirectory(directoryName, path), HttpStatus.CREATED)
    }

    @PostMapping("/recursively")
    fun addDirectoriesRecursively(@RequestParam("path") pathMultiDirectories: String): ResponseEntity<String> {
        return ResponseEntity(
            directoryManagerService.addDirectoriesRecursively(pathMultiDirectories),
            HttpStatus.CREATED
        )
    }

    @PatchMapping
    fun renameDirectory(
        @RequestParam("sourcePath") path: String,
        @RequestParam("newDirName") newName: String
    ): ResponseEntity<String> {
        return ResponseEntity.ok(directoryManagerService.renameDirectory(newName, path))
    }

    @DeleteMapping
    fun deleteDirectory(@RequestParam("path") path: String): ResponseEntity<Unit> {
        return ResponseEntity(directoryManagerService.deleteDirectory(path), HttpStatus.NO_CONTENT)
    }
}