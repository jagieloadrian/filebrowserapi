package com.anjo.filebrowserapi

import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.util.LinkedMultiValueMap
import java.nio.file.Files
import kotlin.test.Test

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["application.max-upload-size=16B"],
)
@ActiveProfiles("web")
class UploadLimitTest(
    @Autowired private val restTemplate: TestRestTemplate,
) {
    @Test
    fun `should return 413 when upload exceeds max upload size`() {
        // given
        val file =
            object : ByteArrayResource(ByteArray(64)) {
                override fun getFilename() = FIRST_FILE_NAME
            }
        val input =
            HttpEntity(
                LinkedMultiValueMap<String, Any>().apply { add("uploadFile", file) },
                HttpHeaders().apply { contentType = MediaType.MULTIPART_FORM_DATA },
            )

        // when
        val response = restTemplate.postForEntity("/api/v1/files", input, String::class.java)

        // then
        response.statusCode.value() shouldBe 413
        response.body shouldContain "\"error\":\"MaxUploadSizeExceededException\""
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
