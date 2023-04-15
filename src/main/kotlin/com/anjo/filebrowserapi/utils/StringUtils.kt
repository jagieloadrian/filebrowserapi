package com.anjo.filebrowserapi.utils

class StringUtils {
    companion object {
        fun formatResponsePath(fullPath: String, defaultPath: String): String {
            return fullPath.substringAfter(defaultPath)
        }
    }
}