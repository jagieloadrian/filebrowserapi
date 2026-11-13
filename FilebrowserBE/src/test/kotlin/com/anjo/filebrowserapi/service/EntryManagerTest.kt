package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import com.anjo.filebrowserapi.TestObjects.THIRD_NAME
import com.anjo.filebrowserapi.TestObjects.signIn
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.AlreadyExistsException
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.exception.QuotaExceededException
import com.anjo.filebrowserapi.model.Entry
import com.anjo.filebrowserapi.model.EntryMeta
import com.anjo.filebrowserapi.model.FileHash
import com.anjo.filebrowserapi.repository.MetadataRepository
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

    private val metadata = mockk<MetadataRepository>(relaxed = true)
    private val quota = mockk<QuotaManager>(relaxed = true)

    private val service by lazy { EntryManager(PathModifierUtils(AppConfiguration(workDir.toString())), metadata, quota) }

    @BeforeEach
    fun signInUser() {
        signIn()
        every { metadata.findUnder(any(), any()) } returns emptyMap()
        every { metadata.findHashes(any(), any()) } returns emptyMap()
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

    @Test
    fun `should copy file and keep source when copyEntry is called with file`() {
        // when
        val result = service.copyEntry("$FIRST_NAME/$FIRST_FILE_NAME", "/$SECOND_FILE_NAME")

        // then
        result shouldBe "/$SECOND_FILE_NAME"
        File(baseFolder, SECOND_FILE_NAME).readText() shouldBe TEST_MESSAGE
        File(baseFolder, "$FIRST_NAME/$FIRST_FILE_NAME").exists() shouldBe true
    }

    @Test
    fun `should copy directory with its content when copyEntry is called with directory`() {
        // when
        val result = service.copyEntry(FIRST_NAME, THIRD_NAME)

        // then
        result shouldBe "/$THIRD_NAME"
        File(baseFolder, "$THIRD_NAME/$FIRST_FILE_NAME").readText() shouldBe TEST_MESSAGE
        File(baseFolder, "$FIRST_NAME/$FIRST_FILE_NAME").exists() shouldBe true
    }

    @Test
    fun `should move directory with its content when moveEntry is called`() {
        // when
        val result = service.moveEntry(FIRST_NAME, "$THIRD_NAME.moved")

        // then
        result shouldBe "/$THIRD_NAME.moved"
        File(baseFolder, "$THIRD_NAME.moved/$FIRST_FILE_NAME").readText() shouldBe TEST_MESSAGE
        File(baseFolder, FIRST_NAME).exists() shouldBe false
    }

    @Test
    fun `should move file into directory when moveEntry is called with file`() {
        // when
        val result = service.moveEntry("$SECOND_NAME.pdf", "$FIRST_NAME/$SECOND_NAME.pdf")

        // then
        result shouldBe "/$FIRST_NAME/$SECOND_NAME.pdf"
        File(baseFolder, "$SECOND_NAME.pdf").exists() shouldBe false
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should throw AlreadyExistsException when target exists`(copy: Boolean) {
        // when & then
        assertThrows<AlreadyExistsException> { transfer(copy, "$SECOND_NAME.pdf", FIRST_NAME) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should throw NotFoundException when source does not exist`(copy: Boolean) {
        // when & then
        assertThrows<NotFoundException> { transfer(copy, SECOND_FILE_NAME, THIRD_NAME) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should throw NotFoundException when target directory does not exist`(copy: Boolean) {
        // when & then
        assertThrows<NotFoundException> { transfer(copy, "$SECOND_NAME.pdf", "$THIRD_NAME/$SECOND_NAME.pdf") }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should throw BadInputException when directory is put inside itself`(copy: Boolean) {
        // when & then
        assertThrows<BadInputException> { transfer(copy, FIRST_NAME, "$FIRST_NAME/$THIRD_NAME") }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should throw BadInputException when base directory is source`(copy: Boolean) {
        // when & then
        assertThrows<BadInputException> { transfer(copy, "/", THIRD_NAME) }
    }

    @Test
    fun `should return description and tags and filter by tag when metadata exists`() {
        // given
        val key = "/$FIRST_NAME/$FIRST_FILE_NAME"
        every { metadata.findUnder("user-1", "") } returns mapOf(key to EntryMeta(key, "notes", listOf("work")))

        // when
        val result = service.listEntries(null, null, null, "WORK")

        // then
        result.map { it.path } shouldContainExactly listOf(key)
        result[0].description shouldBe "notes"
        result[0].tags shouldBe listOf("work")
    }

    @Test
    fun `should save normalized description and tags when updateMeta is called`() {
        // when
        val result = service.updateMeta(FIRST_NAME, "  notes  ", listOf("Work", "work", "Draft"))

        // then
        result.description shouldBe "notes"
        result.tags shouldBe listOf("draft", "work")
        verify(exactly = 1) { metadata.save("user-1", "/$FIRST_NAME", "notes", listOf("draft", "work")) }
    }

    @ParameterizedTest
    @ValueSource(strings = [" lead", "a,b", ""])
    fun `should throw BadInputException when a tag breaks the rule`(tag: String) {
        // when & then
        assertThrows<BadInputException> { service.updateMeta(FIRST_NAME, null, listOf(tag)) }
    }

    @Test
    fun `should throw NotFoundException when updateMeta gets missing entry`() {
        // when & then
        assertThrows<NotFoundException> { service.updateMeta(THIRD_NAME, "notes", emptyList()) }
    }

    @Test
    fun `should copy and relocate metadata when entries are copied and moved`() {
        // when
        service.copyEntry(FIRST_NAME, THIRD_NAME)
        service.moveEntry(THIRD_NAME, "$THIRD_NAME.moved")

        // then
        verify(exactly = 1) { metadata.copy("user-1", "/$FIRST_NAME", "/$THIRD_NAME") }
        verify(exactly = 1) { metadata.relocate("user-1", "/$THIRD_NAME", "/$THIRD_NAME.moved") }
    }

    @Test
    fun `should group files with equal content and hash only files of a shared size`() {
        // given
        File(baseFolder, "copy.txt").writeText(TEST_MESSAGE)
        File(baseFolder, "same-size.txt").writeText(TEST_MESSAGE.reversed())
        File(baseFolder, "unique.txt").writeText("x")

        // when
        val result = service.findDuplicates(null)

        // then
        result.map { it.paths } shouldContainExactly listOf(listOf("/copy.txt", "/$FIRST_NAME/$FIRST_FILE_NAME", "/$SECOND_NAME.pdf"))
        result[0].size shouldBe TEST_MESSAGE.length.toLong()
        verify(exactly = 1) { metadata.saveHashes("user-1", match { hashes -> hashes.size == 4 }) }
    }

    @Test
    fun `should reuse cached hash when size and modification time are unchanged`() {
        // given
        val first = File(baseFolder, "$FIRST_NAME/$FIRST_FILE_NAME")
        val second = File(baseFolder, "$SECOND_NAME.pdf")
        every { metadata.findHashes("user-1", "") } returns
            mapOf(
                "/$FIRST_NAME/$FIRST_FILE_NAME" to
                    FileHash(
                        "/$FIRST_NAME/$FIRST_FILE_NAME",
                        first.length(),
                        first.lastModified(),
                        "cached",
                    ),
                "/$SECOND_NAME.pdf" to FileHash("/$SECOND_NAME.pdf", second.length(), second.lastModified() - 1, "stale"),
            )

        // when
        val result = service.findDuplicates(null)

        // then
        result shouldBe emptyList()
        verify(exactly = 1) { metadata.saveHashes("user-1", match { hashes -> hashes.map { it.path } == listOf("/$SECOND_NAME.pdf") }) }
    }

    @Test
    fun `should reserve the copied size and copy nothing when the quota is exceeded`() {
        // given
        every { quota.reserve(TEST_MESSAGE.length.toLong()) } throws QuotaExceededException("full")

        // when & then
        assertThrows<QuotaExceededException> { service.copyEntry(FIRST_NAME, THIRD_NAME) }
        File(baseFolder, THIRD_NAME).exists() shouldBe false
    }

    private fun transfer(
        copy: Boolean,
        source: String,
        target: String,
    ) = if (copy) service.copyEntry(source, target) else service.moveEntry(source, target)
}
