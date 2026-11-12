package com.anjo.filebrowserapi

import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

object TestObjects {
    const val USER = "user-1"
    const val BASE_FOLDER = "filebrowser/$USER"

    const val FIRST_NAME = "firstName"
    const val SECOND_NAME = "secondName"
    const val THIRD_NAME = "thirdName"
    const val TEST_MESSAGE = "TestMessage"

    const val FIRST_FILE_NAME = "firstName.txt"
    const val SECOND_FILE_NAME = "secondName.txt"

    fun signIn(sub: String = USER) {
        SecurityContextHolder.getContext().authentication = TestingAuthenticationToken(sub, null)
    }
}
