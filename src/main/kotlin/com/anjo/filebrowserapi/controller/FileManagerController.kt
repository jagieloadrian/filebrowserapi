package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.service.FileManagerService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/files")
class FileManagerController(
    private val fileManagerService: FileManagerService
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(@RequestBody uploadFile: MultipartFile, @RequestParam path: String?) : ResponseEntity<String> {
        return ResponseEntity.ok(fileManagerService.uploadFile(uploadFile, path))
    }
}