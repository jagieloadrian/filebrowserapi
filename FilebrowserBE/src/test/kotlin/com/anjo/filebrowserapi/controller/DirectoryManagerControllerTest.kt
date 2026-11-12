package com.anjo.filebrowserapi.controller

import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.service.DirectoryManager
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatusCode
import kotlin.test.Test

@ExtendWith(MockKExtension::class)
class DirectoryManagerControllerTest(
    @MockK
    private val service: DirectoryManager,
) {
    @InjectMockKs
    private lateinit var controller: DirectoryManagerController

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `given request when getDirectoriesTree then return list of string`() {
        // given
        val expected = listOf("/$FIRST_NAME", "/$SECOND_NAME")
        val expectedCode = 200

        every { service.showDirectoriesTree(any()) } returns expected
        // when
        val response = controller.getDirectoriesTree(null)

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatusCode.valueOf(expectedCode)
            response.body shouldContainExactly expected
        }
    }

    @Test
    fun `given directory name when addDirectory then return properly path`() {
        // given
        val expected = "/$FIRST_NAME"
        val expectedCode = 201

        every { service.addDirectory(any(), any()) } returns expected
        // when
        val response = controller.addDirectory(FIRST_NAME, null)

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatusCode.valueOf(expectedCode)
            response.body shouldBe expected
        }
    }

    @Test
    fun `given directory names when addDirectoriesRecursively then return properly path`() {
        // given
        val expected = "/$FIRST_NAME/$SECOND_NAME"
        val expectedCode = 201

        every { service.addDirectoriesRecursively(any()) } returns expected
        // when
        val response = controller.addDirectoriesRecursively("$FIRST_NAME/$SECOND_NAME")

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatusCode.valueOf(expectedCode)
            response.body shouldBe expected
        }
    }

    @Test
    fun `given new name and path when renameDirectory then return properly path`() {
        // given
        val expected = "/$FIRST_NAME/$SECOND_NAME"
        val expectedCode = 200

        every { service.renameDirectory(any(), any()) } returns expected
        // when
        val response = controller.renameDirectory("$FIRST_NAME/$FIRST_NAME", SECOND_NAME)

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatusCode.valueOf(expectedCode)
            response.body shouldBe expected
        }
    }

    @Test
    fun `given path when deleteDirectory then return only statusCode`() {
        // given
        val expectedCode = 204

        every { service.deleteDirectory(any()) } returns Unit
        // when
        val response = controller.deleteDirectory("$FIRST_NAME/$FIRST_NAME")

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatusCode.valueOf(expectedCode)
        }
    }
}
