package com.anjo.filebrowserapi.utils

import org.springframework.util.FileCopyUtils
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

class ByteArrayToMultiPartFile(
    private val input: ByteArray,
    private val name: String,
    private val contentType: String,
) : MultipartFile {
    override fun getInputStream(): InputStream = ByteArrayInputStream(input)

    override fun getName(): String = name

    override fun getOriginalFilename(): String? = name

    override fun getContentType(): String? = contentType

    override fun isEmpty(): Boolean = input.isEmpty()

    override fun getSize(): Long = input.size.toLong()

    override fun getBytes(): ByteArray = input

    override fun transferTo(dest: File) {
        FileCopyUtils.copy(input, dest)
    }
}
