package com.anjo.filebrowserapi

import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.io.ByteArrayOutputStream
import java.io.PrintStream

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

    inline fun captureOutput(block: () -> Unit): String {
        val out = System.out
        val err = System.err
        val buffer = ByteArrayOutputStream()
        val capture = PrintStream(buffer, true, Charsets.UTF_8)
        System.setOut(capture)
        System.setErr(capture)
        try {
            block()
        } finally {
            System.setOut(out)
            System.setErr(err)
        }
        return buffer.toString(Charsets.UTF_8)
    }
}
