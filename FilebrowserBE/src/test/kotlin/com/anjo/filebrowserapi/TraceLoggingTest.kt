package com.anjo.filebrowserapi

import com.anjo.filebrowserapi.TestObjects.captureOutput
import com.anjo.filebrowserapi.service.FileManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.nio.file.Files

private const val TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736"

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("web")
@ApplyExtension(SpringExtension::class)
class TraceLoggingTest : FunSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var fileManager: FileManager

    init {
        test("should log trace id from traceparent when request fails") {
            // given
            every { fileManager.showFilesTree(null) } throws RuntimeException("boom")

            // when
            val output =
                captureOutput {
                    mockMvc.get("/api/v1/files") {
                        header("traceparent", "00-$TRACE_ID-00f067aa0ba902b7-01")
                        header(HttpHeaders.AUTHORIZATION, "Bearer ${TestTokens.token(TestTokens.READER)}")
                    }
                }

            // then
            output shouldContain TRACE_ID
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            val workDir = Files.createTempDirectory("filebrowser-trace")
            registry.add("application.path") { workDir.toString() }
            TestTokens.register(registry)
        }
    }
}
