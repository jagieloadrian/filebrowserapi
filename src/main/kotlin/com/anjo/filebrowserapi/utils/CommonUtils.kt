package com.anjo.filebrowserapi.utils

class CommonUtils {
    companion object {
        const val EIGHT = 8

        fun formatResponsePath(fullPath: String, defaultPath: String): String {
            return fullPath.substringAfter(defaultPath)
        }
    }
}