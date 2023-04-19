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
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File


@ExtendWith(MockKExtension::class)
class DirectoryManagerServiceTest {

    private val appConfiguration =
        AppConfiguration(baseDiskPath = "${System.getProperty("user.dir")}$PATH_TO_RESOURCES")

    private val modifierUtils = PathModifierUtils(appConfiguration)

    @InjectMockKs
    private lateinit var service: DirectoryManagerService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @AfterEach
    fun cleanUp() {
        val files = File("${System.getProperty("user.dir")}$PATH_TO_RESOURCES/$BASE_FOLDER").walk().filter { file ->
            file.isDirectory
        }
            .map { file ->
                file.path
            }
            .drop(1)
            .sortedDescending()
            .toList()

        files.forEach { s ->
            val temp = File(s)
            temp.deleteRecursively()
        }
    }

    @Test
    fun `given empty base directory and null path when show directories tree then return empty list`() {
        //when
        val result = service.showDirectoriesTree(null)

        //then
        result shouldHaveSize 0
    }

    @Test
    fun `given base directory with 2 folders and null path when show directories tree then return properly list`() {
        createFolder(FIRST_NAME)
        createFolder(SECOND_NAME)

        val expected = listOf("/$FIRST_NAME", "/$SECOND_NAME")

        //when
        val result = service.showDirectoriesTree(null)

        //then
        result shouldContainExactly expected
    }

    @Test
    fun `given base directory with few folders and path when show directories tree from specific path then return properly list`() {
        createFolder(FIRST_NAME)
        createFolder("$FIRST_NAME/$SECOND_NAME")
        createFolder("$FIRST_NAME/$THIRD_NAME")

        val expected = listOf("/$FIRST_NAME/$SECOND_NAME", "/$FIRST_NAME/$THIRD_NAME")

        //when
        val result = service.showDirectoriesTree(FIRST_NAME)

        //then
        result shouldContainExactly expected
    }

    @Test
    fun `given directory name and path null when create directory then return path`() {
        //given
        val name = FIRST_NAME
        val expected = "/$FIRST_NAME"

        //when
        val result = service.addDirectory(name, null)

        //then
        assertSoftly {
            result shouldBe expected
        }
    }

    @Test
    fun `given directory name and wrong path when create directory then return CannotRealizeException`() {
        //given
        val name = FIRST_NAME
        val wrongPath = "/$SECOND_NAME"
        val expectedMessage = "Path: $wrongPath to create file or directory is not exist"

        //when
        val exception = shouldThrow<CannotRealizeException> { service.addDirectory(name, wrongPath) }

        //then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `given exist directory name and null path when create directory then return CannotRealizeException`() {
        //given
        val name = FIRST_NAME
        createFolder(name)
        val expectedMessage = "Directory with name: $name in path:"

        //when
        val exception = shouldThrow<CannotRealizeException> { service.addDirectory(name, null) }

        //then
        exception.message shouldStartWith expectedMessage
    }

    @Test
    fun `given directories names when create directory recursively then return path to last directory`() {
        //given
        val names = "$FIRST_NAME/$SECOND_NAME/$THIRD_NAME"
        val expected = "/$FIRST_NAME/$SECOND_NAME/$THIRD_NAME"

        //when
        val result = service.addDirectoriesRecursively(names)

        //then
        assertSoftly {
            result shouldBe expected
        }
    }

    @Test
    fun `given name without slash when rename directory then return properly path`() {
        //given
        val name = SECOND_NAME
        val expected = "/$SECOND_NAME"
        val givenPath = FIRST_NAME

        createFolder(givenPath)
        //when
        val result = service.renameDirectory(name, givenPath)

        //then
        assertSoftly {
            result shouldBe expected
        }
    }

    @Test
    fun `given name with slash when rename directory then return properly path`() {
        //given
        val name = "/$THIRD_NAME"
        val expected = "/$THIRD_NAME"
        val givenPath = FIRST_NAME

        createFolder(givenPath)
        //when
        val result = service.renameDirectory(name, givenPath)

        //then
        assertSoftly {
            result shouldBe expected
        }
    }

    @Test
    fun `given name and non existing path when rename directory then return CannotRealizeException`() {
        //given
        val name = "/$THIRD_NAME"
        val givenPath = FIRST_NAME
        val expectedMessage = "Cannot rename not existing directory"

        //when
        val exception = shouldThrow<CannotRealizeException> { service.renameDirectory(name, givenPath) }

        //then
        exception.message shouldBe expectedMessage
    }

    @Test
    fun `given path when delete folder then result not throw error`() {
        //given
        val givenPath = FIRST_NAME
        val tempFolder = createFolder(givenPath)
        tempFolder.mkdir()

        //when
        service.deleteDirectory(givenPath)

        //then
        assertSoftly {
            tempFolder.exists() shouldBe false
        }
    }

    @Test
    fun `given path to not empty folder when delete folder then return CannotRealizeException`() {
        //given
        val givenPath = FIRST_NAME
        val tempFolder = createFolder(givenPath)
        tempFolder.mkdir()
        val file = File("$PATH_TO_BASE_FOLDER/$givenPath/data.txt")
        file.createNewFile()
        val expectedMessage = "Cannot remove not empty directory"

        //when
        val exception = shouldThrow<CannotRealizeException> { service.deleteDirectory(givenPath) }

        //then
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