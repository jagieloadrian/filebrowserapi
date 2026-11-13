package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.service.FileManager
import com.anjo.filebrowserapi.validation.ValidName
import org.springframework.context.annotation.Profile
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.charset.StandardCharsets

@RestController
@Profile("web")
@RequestMapping("/api/v1/files")
class FileManagerController(
    private val fileManager: FileManager,
) {
    @GetMapping
    fun getFilesTree(
        @RequestParam("path", required = false)path: String?,
    ): ResponseEntity<List<String>> = ResponseEntity.ok(fileManager.showFilesTree(path))

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestPart("uploadFile") uploadFile: MultipartFile,
        @RequestParam path: String?,
        @RequestParam(defaultValue = "false") overwrite: Boolean,
    ): ResponseEntity<String> =
        ResponseEntity.ok(fileManager.uploadFile(uploadFile.originalFilename, uploadFile.inputStream, path, overwrite))

    @GetMapping("/{filename:.+}")
    fun getFile(
        @PathVariable filename: String,
        @RequestParam("path", required = false) path: String?,
    ): ResponseEntity<Resource> = fileManager.getFile(filename, path).download()

    @PatchMapping
    fun renameFile(
        @RequestParam("sourcePath") path: String,
        @RequestParam("newFileName") @ValidName newFileName: String,
    ): ResponseEntity<String> = ResponseEntity.ok(fileManager.renameFile(newFileName, path))

    @DeleteMapping("/{filename:.+}")
    fun deleteFile(
        @PathVariable filename: String,
        @RequestParam("path", required = false) path: String?,
    ): ResponseEntity<Unit> = ResponseEntity(fileManager.deleteFile(filename, path), HttpStatus.NO_CONTENT)
}

internal fun File.download(): ResponseEntity<Resource> =
    ResponseEntity
        .ok()
        .contentType(MediaTypeFactory.getMediaType(name).orElse(MediaType.APPLICATION_OCTET_STREAM))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition
                .attachment()
                .filename(name, StandardCharsets.UTF_8)
                .build()
                .toString(),
        ).body(FileSystemResource(this))
