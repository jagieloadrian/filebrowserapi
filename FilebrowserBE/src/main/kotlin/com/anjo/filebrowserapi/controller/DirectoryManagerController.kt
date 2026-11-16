package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.service.DirectoryManager
import com.anjo.filebrowserapi.validation.ValidName
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("web")
@RequestMapping("/api/v1/directories")
class DirectoryManagerController(
    private val directoryManager: DirectoryManager,
) {
    @GetMapping
    fun getDirectoriesTree(
        @RequestParam("path", required = false) path: String?,
    ): List<String> = directoryManager.showDirectoriesTree(path)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addDirectory(
        @RequestParam("directoryName") @ValidName directoryName: String,
        @RequestParam("path", required = false) path: String?,
    ): String = directoryManager.addDirectory(directoryName, path)

    @PostMapping("/recursively")
    @ResponseStatus(HttpStatus.CREATED)
    fun addDirectoriesRecursively(
        @RequestParam("path") pathMultiDirectories: String,
    ): String = directoryManager.addDirectoriesRecursively(pathMultiDirectories)

    @PatchMapping
    fun renameDirectory(
        @RequestParam("sourcePath") path: String,
        @RequestParam("newDirName") @ValidName newDirName: String,
    ): String = directoryManager.renameDirectory(newDirName, path)

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteDirectory(
        @RequestParam("path") path: String,
    ) = directoryManager.deleteDirectory(path)

    @DeleteMapping("/recursively")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteDirectoryRecursively(
        @RequestParam("path") path: String,
    ) = directoryManager.deleteDirectoryRecursively(path)
}
