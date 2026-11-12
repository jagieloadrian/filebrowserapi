package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.PATH_TO_BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.PATH_TO_RESOURCES
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.TestObjects.THIRD_NAME
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.CannotRealizeException
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class DirectoryManagerTest {
    private val appConfiguration =
        AppConfiguration(path = "${System.getProperty("user.dir")}$PATH_TO_RESOURCES")

    private val modifierUtils = PathModifierUtils(appConfiguration)

    @InjectMockKs
    private lateinit var service: DirectoryManager

    @AfterTest
    fun cleanUp() {
        val files =
            File("${System.getProperty("user.dir")}$PATH_TO_RESOURCES/$BASE_FOLDER")
                .walk()
                .filter { file ->
                    file.isDirectory
                }.map { file ->
                    file.path
                }.drop(1)
                .sortedDescending()
                .toList()

        files.forEach { s ->
            val temp = File(s)
            temp.deleteRecursively()
        }
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
    fun `should throw CannotRealizeException when creating directory in missing path`() {
        // given
        val name = FIRST_NAME
        val wrongPath = "/$SECOND_NAME"
        val expectedMessage = "Path: $wrongPath to create file or directory is not exist"

        // when
        val exception = shouldThrow<CannotRealizeException> { service.addDirectory(name, wrongPath) }

        // then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `should throw CannotRealizeException when directory already exists`() {
        // given
        val name = FIRST_NAME
        createFolder(name)
        val expectedMessage = "Directory with name: $name in path:"

        // when
        val exception = shouldThrow<CannotRealizeException> { service.addDirectory(name, null) }

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
    }

    @Test
    fun `should return renamed path when name has slash`() {
        // given
        val name = "/$THIRD_NAME"
        val expected = "/$THIRD_NAME"
        val givenPath = FIRST_NAME

        createFolder(givenPath)
        // when
        val result = service.renameDirectory(name, givenPath)

        // then
        assertSoftly {
            result shouldBe expected
        }
    }

    @Test
    fun `should throw CannotRealizeException when renaming missing directory`() {
        // given
        val name = "/$THIRD_NAME"
        val givenPath = FIRST_NAME
        val expectedMessage = "Cannot rename not existing directory"

        // when
        val exception = shouldThrow<CannotRealizeException> { service.renameDirectory(name, givenPath) }

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
    fun `should throw CannotRealizeException when deleting non-empty folder`() {
        // given
        val givenPath = FIRST_NAME
        val tempFolder = createFolder(givenPath)
        tempFolder.mkdir()
        val file = File("$PATH_TO_BASE_FOLDER/$givenPath/data.txt")
        file.createNewFile()
        val expectedMessage = "Cannot remove not empty directory"

        // when
        val exception = shouldThrow<CannotRealizeException> { service.deleteDirectory(givenPath) }

        // then
        exception.message shouldBe expectedMessage
    }

    private fun createFolder(path: String): File {
        val tempFolder = File("$PATH_TO_BASE_FOLDER/$path")
        tempFolder.mkdir()
        return tempFolder
    }

    companion object {
        @JvmStatic
        @AfterAll
        fun clearAfterTests() {
            val baseDirectory = File(PATH_TO_BASE_FOLDER)
            baseDirectory.deleteRecursively()
        }
    }
}
