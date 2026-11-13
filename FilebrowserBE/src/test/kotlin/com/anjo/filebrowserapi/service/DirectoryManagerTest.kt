package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.TestObjects.THIRD_NAME
import com.anjo.filebrowserapi.TestObjects.signIn
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.AlreadyExistsException
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotEmptyException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.repository.MetadataRepository
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.springframework.security.core.context.SecurityContextHolder
import java.io.File
import java.nio.file.Path
import kotlin.test.Test

class DirectoryManagerTest {
    @TempDir
    private lateinit var workDir: Path

    private val baseFolder: String get() = "$workDir/$BASE_FOLDER"

    private val metadata = mockk<MetadataRepository>(relaxed = true)

    private val utils by lazy { PathModifierUtils(AppConfiguration(workDir.toString())) }

    private val service by lazy { DirectoryManager(utils, metadata, TrashManager(utils, metadata, mockk(relaxed = true))) }

    @BeforeEach
    fun createBaseFolder() {
        File(baseFolder).mkdirs()
    }

    @BeforeEach
    fun signInUser() {
        signIn()
    }

    @AfterEach
    fun signOut() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should return empty list when base directory is empty and path is null`() {
        // when
        val result = service.showDirectoriesTree(null)

        // then
        result shouldHaveSize 0
    }

    @Test
    fun `should return directories when base directory has two folders and path is null`() {
        createFolder(FIRST_NAME)
        createFolder(SECOND_NAME)

        val expected = listOf("/$FIRST_NAME", "/$SECOND_NAME")

        // when
        val result = service.showDirectoriesTree(null)

        // then
        result shouldContainExactly expected
    }

    @Test
    fun `should return subdirectories when path points to folder`() {
        createFolder(FIRST_NAME)
        createFolder("$FIRST_NAME/$SECOND_NAME")
        createFolder("$FIRST_NAME/$THIRD_NAME")

        val expected = listOf("/$FIRST_NAME/$SECOND_NAME", "/$FIRST_NAME/$THIRD_NAME")

        // when
        val result = service.showDirectoriesTree(FIRST_NAME)

        // then
        result shouldContainExactly expected
    }

    @Test
    fun `should return directory path when creating directory without path`() {
        // given
        val name = FIRST_NAME
        val expected = "/$FIRST_NAME"

        // when
        val result = service.addDirectory(name, null)

        // then
        assertSoftly {
            result shouldBe expected
        }
    }

    @Test
    fun `should throw NotFoundException when creating directory in missing path`() {
        // given
        val name = FIRST_NAME
        val wrongPath = "/$SECOND_NAME"
        val expectedMessage = "Path: $wrongPath to create file or directory is not exist"

        // when
        val exception = shouldThrow<NotFoundException> { service.addDirectory(name, wrongPath) }

        // then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `should throw AlreadyExistsException when directory already exists`() {
        // given
        val name = FIRST_NAME
        createFolder(name)
        val expectedMessage = "Directory with name: $name in path:"

        // when
        val exception = shouldThrow<AlreadyExistsException> { service.addDirectory(name, null) }

        // then
        exception.message shouldStartWith expectedMessage
    }

    @Test
    fun `should return last directory path when creating directories recursively`() {
        // given
        val names = "$FIRST_NAME/$SECOND_NAME/$THIRD_NAME"
        val expected = "/$FIRST_NAME/$SECOND_NAME/$THIRD_NAME"

        // when
        val result = service.addDirectoriesRecursively(names)

        // then
        assertSoftly {
            result shouldBe expected
        }
    }

    @Test
    fun `should return renamed path when name has no slash`() {
        // given
        val name = SECOND_NAME
        val expected = "/$SECOND_NAME"
        val givenPath = FIRST_NAME

        createFolder(givenPath)
        // when
        val result = service.renameDirectory(name, givenPath)

        // then
        assertSoftly {
            result shouldBe expected
        }
        verify(exactly = 1) { metadata.relocate("user-1", "/$FIRST_NAME", expected) }
    }

    @Test
    fun `should throw BadInputException when new name has slash`() {
        // given
        createFolder(FIRST_NAME)

        // when & then
        shouldThrow<BadInputException> { service.renameDirectory("/$THIRD_NAME", FIRST_NAME) }
        File("$baseFolder/$FIRST_NAME").isDirectory shouldBe true
    }

    @Test
    fun `should throw NotFoundException when renaming missing directory`() {
        // given
        val name = THIRD_NAME
        val givenPath = FIRST_NAME
        val expectedMessage = "Cannot rename not existing directory"

        // when
        val exception = shouldThrow<NotFoundException> { service.renameDirectory(name, givenPath) }

        // then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `should delete folder when folder is empty`() {
        // given
        val givenPath = FIRST_NAME
        val tempFolder = createFolder(givenPath)
        tempFolder.mkdir()

        // when
        service.deleteDirectory(givenPath)

        // then
        assertSoftly {
            tempFolder.exists() shouldBe false
        }
    }

    @Test
    fun `should throw NotEmptyException when deleting non-empty folder`() {
        // given
        val givenPath = FIRST_NAME
        val tempFolder = createFolder(givenPath)
        tempFolder.mkdir()
        val file = File("$baseFolder/$givenPath/data.txt")
        file.createNewFile()
        val expectedMessage = "Cannot remove not empty directory"

        // when
        val exception = shouldThrow<NotEmptyException> { service.deleteDirectory(givenPath) }

        // then
        exception.message shouldBe expectedMessage
    }

    private fun createFolder(path: String): File {
        val tempFolder = File("$baseFolder/$path")
        tempFolder.mkdir()
        return tempFolder
    }

    @Test
    fun `should throw BadInputException when showDirectoriesTree leaves base directory`() {
        // when & then
        shouldThrow<BadInputException> { service.showDirectoriesTree("../..") }
    }

    @Test
    fun `should throw BadInputException when addDirectory leaves base directory`() {
        // when & then
        shouldThrow<BadInputException> { service.addDirectory("../$FIRST_NAME", null) }
    }

    @Test
    fun `should throw BadInputException when addDirectoriesRecursively leaves base directory`() {
        // when & then
        shouldThrow<BadInputException> { service.addDirectoriesRecursively("../../$FIRST_NAME") }
    }

    @Test
    fun `should throw BadInputException when renameDirectory leaves base directory`() {
        // when & then
        shouldThrow<BadInputException> { service.renameDirectory("../$SECOND_NAME", FIRST_NAME) }
    }

    @Test
    fun `should throw BadInputException when deleteDirectory leaves base directory`() {
        // when & then
        shouldThrow<BadInputException> { service.deleteDirectory("../") }
    }

    @Test
    fun `should throw BadInputException when deleting base directory`() {
        // when & then
        shouldThrow<BadInputException> { service.deleteDirectory("") }
    }

    @Test
    fun `should delete directory with its content when deleteDirectoryRecursively is called`() {
        // given
        File("$baseFolder/$FIRST_NAME/$SECOND_NAME").mkdirs()
        File("$baseFolder/$FIRST_NAME/$SECOND_NAME/file.txt").writeText("data")

        // when
        service.deleteDirectoryRecursively(FIRST_NAME)

        // then
        verify(exactly = 1) { metadata.relocate("user-1", "/$FIRST_NAME", match { it.startsWith("trash:") }) }
        File("$baseFolder/$FIRST_NAME").exists() shouldBe false
        File(baseFolder).exists() shouldBe true
    }

    @Test
    fun `should throw NotFoundException when deleteDirectoryRecursively gets missing directory`() {
        // when & then
        shouldThrow<NotFoundException> { service.deleteDirectoryRecursively(FIRST_NAME) }
    }

    @Test
    fun `should throw BadInputException when deleteDirectoryRecursively gets a file`() {
        // given
        File("$baseFolder/file.txt").writeText("data")

        // when & then
        shouldThrow<BadInputException> { service.deleteDirectoryRecursively("file.txt") }
    }

    @Test
    fun `should throw BadInputException when deleteDirectoryRecursively targets base directory`() {
        // when & then
        shouldThrow<BadInputException> { service.deleteDirectoryRecursively("/") }
        File(baseFolder).exists() shouldBe true
    }
}
