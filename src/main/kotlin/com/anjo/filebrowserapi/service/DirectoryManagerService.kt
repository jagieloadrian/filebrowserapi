package com.anjo.filebrowserapi.service

import org.springframework.stereotype.Service

@Service
class DirectoryManagerService {

    fun currentDir():String {
        return System.getProperty("user.dir")
    }
}