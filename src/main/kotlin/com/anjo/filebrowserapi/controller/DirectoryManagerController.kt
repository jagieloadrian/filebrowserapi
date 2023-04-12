package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.service.DirectoryManagerService
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
@RequestMapping("/api/v1")
class DirectoryManagerController(
    private val directoryManagerService: DirectoryManagerService
) {

    @GetMapping("/directories")
    fun getDirectoriesTree(@RequestParam("path", required = false)path:String?): ResponseEntity<List<String>> {
        return ResponseEntity.ok(directoryManagerService.showDirectoriesTree(path))
    }

    @PostMapping("/directories")
    fun addDirectory(
        @RequestParam("directoryName") directoryName: String,
        @RequestParam("path", required = false) path: String?
    ):ResponseEntity<String> {
        return ResponseEntity(directoryManagerService.addDirectory(directoryName, path), HttpStatus.CREATED)
    }

    @PostMapping("/directories/recursively")
    fun addDirectoriesRecursively(@RequestParam("path") pathMultiDirectories:String):ResponseEntity<String> {
        return ResponseEntity(directoryManagerService.addDirectoriesRecursively(pathMultiDirectories), HttpStatus.CREATED)
    }

    @PatchMapping("/directories")
    fun renameDirectory(
        @RequestParam("sourcePath") path: String,
        @RequestParam("newDirName") newName: String
    ) :ResponseEntity<String>{
        return ResponseEntity.ok(directoryManagerService.renameDirectory(newName, path))
    }

    @DeleteMapping("directories")
    fun deleteDirectory(@RequestParam("path") path:String):ResponseEntity<Unit> {
        return ResponseEntity(directoryManagerService.deleteDirectory(path), HttpStatus.NO_CONTENT)
    }
}