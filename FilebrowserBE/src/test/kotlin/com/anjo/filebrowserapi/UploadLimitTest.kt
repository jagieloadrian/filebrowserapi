package com.anjo.filebrowserapi

import com.anjo.filebrowserapi.TestObjects.FIRST_FILE_NAME
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.util.LinkedMultiValueMap
import java.nio.file.Files

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["application.max-upload-size=16B"],
)
@AutoConfigureRestTestClient
@ActiveProfiles("web")
@ApplyExtension(SpringExtension::class)
class UploadLimitTest : FunSpec() {
    @Autowired
    private lateinit var client: RestTestClient

    init {
        test("should return 413 when upload exceeds max upload size") {
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
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${TestTokens.token(TestTokens.WRITER)}")
                    .body(input)
                    .exchange()
                    .returnResult(String::class.java)

            // then
            result.status.value() shouldBe 413
            result.responseBody shouldContain "\"error\":\"MaxUploadSizeExceededException\""
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            val workDir = Files.createTempDirectory("filebrowser-upload-limit")
            registry.add("application.path") { workDir.toString() }
            TestTokens.register(registry)
        }
    }
}
