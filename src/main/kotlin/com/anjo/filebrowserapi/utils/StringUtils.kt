package com.anjo.filebrowserapi.utils

class StringUtils {
    companion object {
        fun formatDirectoryPath(fullPath: String, defaultPath: String): String {
            return fullPath.substringAfter(defaultPath)
        }
    }
}