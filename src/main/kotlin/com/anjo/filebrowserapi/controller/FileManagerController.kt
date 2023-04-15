package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.service.FileManagerService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/files")
class FileManagerController(
    private val fileManagerService: FileManagerService
) {
}