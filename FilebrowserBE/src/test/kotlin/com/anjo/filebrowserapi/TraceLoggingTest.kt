package com.anjo.filebrowserapi

import com.anjo.filebrowserapi.service.FileManager
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.nio.file.Files
import kotlin.test.Test

private const val TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736"

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("web")
@ExtendWith(OutputCaptureExtension::class)
class TraceLoggingTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean
    private lateinit var fileManager: FileManager

    @Test
    fun `should log trace id from traceparent when request fails`(output: CapturedOutput) {
        // given
        every { fileManager.showFilesTree(null) } throws RuntimeException("boom")

        // when
        mockMvc.get("/api/v1/files") { header("traceparent", "00-$TRACE_ID-00f067aa0ba902b7-01") }

        // then
        output.out shouldContain TRACE_ID
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            val workDir = Files.createTempDirectory("filebrowser-trace")
            registry.add("application.path") { workDir.toString() }
        }
    }
}
