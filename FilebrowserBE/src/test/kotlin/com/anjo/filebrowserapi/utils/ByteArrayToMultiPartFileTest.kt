package com.anjo.filebrowserapi.utils

import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test

class ByteArrayToMultiPartFileTest {
    private val content = TEST_MESSAGE.toByteArray()
    private val file = ByteArrayToMultiPartFile(content, FIRST_FILE_NAME, CONTENT_TYPE)

    @Test
    fun `should expose content and metadata when created from bytes`() {
        // then
        assertSoftly(file) {
            name shouldBe FIRST_FILE_NAME
            originalFilename shouldBe FIRST_FILE_NAME
            contentType shouldBe CONTENT_TYPE
            isEmpty shouldBe false
            size shouldBe content.size.toLong()
            bytes shouldBe content
            inputStream.readBytes() shouldBe content
        }
    }

    @Test
    fun `should report empty when bytes are empty`() {
        // when
        val result = ByteArrayToMultiPartFile(ByteArray(0), FIRST_FILE_NAME, CONTENT_TYPE)

        // then
        result.isEmpty shouldBe true
    }

    @Test
    fun `should write content to destination when transferTo is called`(
        @TempDir workDir: Path,
    ) {
        // given
        val destination = workDir.resolve(FIRST_FILE_NAME).toFile()

        // when
        file.transferTo(destination)

        // then
        destination.readBytes() shouldBe content
    }

    private companion object {
        const val CONTENT_TYPE = "text/plain"
    }
}
