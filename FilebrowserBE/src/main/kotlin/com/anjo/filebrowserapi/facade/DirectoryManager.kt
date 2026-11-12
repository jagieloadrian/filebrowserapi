package com.anjo.filebrowserapi.facade

interface DirectoryManager {
    fun showDirectoriesTree(path: String?): List<String>

    fun addDirectory(
        directoryName: String,
        path: String?,
    ): String

    fun addDirectoriesRecursively(pathMultiDirectories: String): String

    fun renameDirectory(
        newName: String,
        path: String,
    ): String

    fun deleteDirectory(path: String)
}
