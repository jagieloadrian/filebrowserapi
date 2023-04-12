package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.PATH_TO_RESOURCES
import com.anjo.filebrowserapi.configuration.AppConfiguration
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File


@ExtendWith(MockKExtension::class)
class DirectoryManagerServiceTest {

    private val appConfiguration =
        AppConfiguration(baseDiskPath = "${System.getProperty("user.dir")}$PATH_TO_RESOURCES")

    @InjectMockKs
    private lateinit var service: DirectoryManagerService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @AfterEach
    fun cleanUp() {
        val files = File("${System.getProperty("user.dir")}$PATH_TO_RESOURCES/filebrowser").walk().filter { file ->
            file.isDirectory
        }
            .map { file ->
                println(file.path)
                file.path
            }
            .drop(1)
            .sortedDescending()
            .toList()

        files.forEach {
            s -> val temp = File(s)
            temp.delete()
        }
    }

    @Test
    fun `given directory name and path null when create directory then return path`() {
        //given
        val name = "test"
        val expected = "/test"

        //when
        val result = service.addDirectory(name, null)

        //then
        assertSoftly {
            result shouldBe expected
        }
    }

    @Test
    fun `given directories names when create directory recursively then return path to last directory`() {
        //given
        val name = "test/adam/test"
        val expected = "/test/adam/test"

        //when
        val result = service.addDirectoriesRecursively(name)

        //then
        assertSoftly {
            result shouldBe expected
        }
    }


}