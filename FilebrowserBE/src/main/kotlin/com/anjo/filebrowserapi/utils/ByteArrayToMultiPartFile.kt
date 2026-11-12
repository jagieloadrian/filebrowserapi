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

    override fun getInputStream(): InputStream {
        return ByteArrayInputStream(input)
    }

    override fun getName(): String {
        return name
    }

    override fun getOriginalFilename(): String? {
        return name
    }

    override fun getContentType(): String? {
        return contentType
    }

    override fun isEmpty(): Boolean {
        return input.isEmpty()
    }

    override fun getSize(): Long {
        return input.size.toLong()
    }

    override fun getBytes(): ByteArray {
        return input
    }

    override fun transferTo(dest: File) {
        FileCopyUtils.copy(input, dest)
    }
}