package com.anjo.filebrowserapi.facade

import org.springframework.web.multipart.MultipartFile

interface FileManager {
    fun showFilesTree(path: String?): List<String>
    fun uploadFile(uploadFile:MultipartFile, path: String?): String
    fun getFile(fileName:String, path: String?): ByteArray
    fun deleteFile(fileName:String, path: String?)
}