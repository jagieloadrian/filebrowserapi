package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.TestObjects.signIn
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.Entry
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.security.core.context.SecurityContextHolder
import java.io.File
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test

class EntryManagerTest {
    @TempDir
    private lateinit var workDir: Path

    private val baseFolder: File get() = File("$workDir/$BASE_FOLDER")

    private val service by lazy { EntryManager(PathModifierUtils(AppConfiguration(workDir.toString()))) }

    @BeforeEach
    fun signInUser() {
        signIn()
        File(baseFolder, FIRST_NAME).mkdirs()
        File(baseFolder, "$FIRST_NAME/$FIRST_FILE_NAME").writeText(TEST_MESSAGE)
        File(baseFolder, "$SECOND_NAME.pdf").writeText(TEST_MESSAGE)
    }

    @AfterEach
    fun signOut() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should return files and directories with metadata when path is null`() {
        // given
        val file = File(baseFolder, "$FIRST_NAME/$FIRST_FILE_NAME")

        // when
        val result = service.listEntries(null, null, null)

        // then
        result.map { it.path } shouldContainExactly listOf("/$FIRST_NAME", "/$FIRST_NAME/$FIRST_FILE_NAME", "/$SECOND_NAME.pdf")
        result[0].directory shouldBe true
        result[0].contentType shouldBe null
        result[1] shouldBe
            Entry(
                path = "/$FIRST_NAME/$FIRST_FILE_NAME",
                directory = false,
                size = TEST_MESSAGE.length.toLong(),
                modified = Instant.ofEpochMilli(file.lastModified()),
                contentType = "text/plain",
            )
    }

    @Test
    fun `should return entries below path when path is set`() {
        // when
        val result = service.listEntries(FIRST_NAME, null, null)

        // then
        result.map { it.path } shouldContainExactly listOf("/$FIRST_NAME/$FIRST_FILE_NAME")
    }

    @Test
    fun `should return entries whose name contains the query ignoring case when name is set`() {
        // when
        val result = service.listEntries(null, "FIRSTname", null)

        // then
        result.map { it.path } shouldContainExactly listOf("/$FIRST_NAME", "/$FIRST_NAME/$FIRST_FILE_NAME")
    }

    @ParameterizedTest
    @ValueSource(strings = ["pdf", ".PDF"])
    fun `should return only files with the extension when extension is set`(extension: String) {
        // when
        val result = service.listEntries(null, null, extension)

        // then
        result.map { it.path } shouldContainExactly listOf("/$SECOND_NAME.pdf")
    }

    @Test
    fun `should throw NotFoundException when directory does not exist`() {
        // when & then
        assertThrows<NotFoundException> { service.listEntries(SECOND_FILE_NAME, null, null) }
    }
}
