package com.anjo.filebrowserapi

import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.util.LinkedMultiValueMap
import java.nio.file.Files
import kotlin.test.Test

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["application.max-upload-size=16B"],
)
@AutoConfigureRestTestClient
@ActiveProfiles("web")
class UploadLimitTest(
    @Autowired private val client: RestTestClient,
) {
    @Test
    fun `should return 413 when upload exceeds max upload size`() {
        // given
        val file =
            object : ByteArrayResource(ByteArray(64)) {
                override fun getFilename() = FIRST_FILE_NAME
            }
        val input = LinkedMultiValueMap<String, Any>().apply { add("uploadFile", file) }

        // when
        val result =
            client
                .post()
                .uri("/api/v1/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(input)
                .exchange()
                .returnResult(String::class.java)

        // then
        result.status.value() shouldBe 413
        result.responseBody shouldContain "\"error\":\"MaxUploadSizeExceededException\""
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            val workDir = Files.createTempDirectory("filebrowser-upload-limit")
            registry.add("application.path") { workDir.toString() }
        }
    }
}
