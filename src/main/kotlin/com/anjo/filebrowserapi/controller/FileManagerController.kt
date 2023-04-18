package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.service.FileManagerService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/files")
class FileManagerController(
    private val fileManagerService: FileManagerService
) {

    @GetMapping
    fun getFilesTree(@RequestParam("path", required = false)path: String?):ResponseEntity<List<String>> {
        return ResponseEntity.ok(fileManagerService.showFilesTree(path))
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(@RequestBody uploadFile: MultipartFile, @RequestParam path: String?) : ResponseEntity<String> {
        return ResponseEntity.ok(fileManagerService.uploadFile(uploadFile, path))
    }

    @GetMapping("/{filename:.+}",
        produces = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun getFile(@PathVariable filename:String, @RequestParam ("path", required = false) path: String? ):ResponseEntity<ByteArray>{
        return ResponseEntity.ok(fileManagerService.getFile(filename, path))
    }

    @DeleteMapping("/{filename:.+}")
    fun deleteFile(@PathVariable filename:String, @RequestParam ("path", required = false) path: String? ):ResponseEntity<Unit>{
        return ResponseEntity(fileManagerService.deleteFile(filename, path), HttpStatus.NO_CONTENT)
    }
}