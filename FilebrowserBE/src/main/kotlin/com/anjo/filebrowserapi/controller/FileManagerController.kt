package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.service.FileManagerService
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@Profile("web")
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
        //TODO add resolving producing type :)
        return ResponseEntity.ok(fileManagerService.getFile(filename, path))
    }

    @DeleteMapping("/{filename:.+}")
    fun deleteFile(@PathVariable filename:String, @RequestParam ("path", required = false) path: String? ):ResponseEntity<Unit>{
        return ResponseEntity(fileManagerService.deleteFile(filename, path), HttpStatus.NO_CONTENT)
    }
}