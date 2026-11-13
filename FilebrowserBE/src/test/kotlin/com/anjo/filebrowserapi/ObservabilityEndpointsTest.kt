package com.anjo.filebrowserapi

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.nio.file.Files

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMetrics
@ActiveProfiles("web")
@ApplyExtension(SpringExtension::class)
class ObservabilityEndpointsTest : FunSpec() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    init {
        test("should report UP when liveness probe is called") {
            // when
            val response = mockMvc.get("/health/live").andReturn().response

            // then
            response.status shouldBe 200
            response.contentAsString shouldContain "\"status\":\"UP\""
        }

        test("should report UP when readiness probe is called") {
            // when
            val response = mockMvc.get("/health/ready").andReturn().response

            // then
            response.status shouldBe 200
            response.contentAsString shouldContain "\"status\":\"UP\""
        }

        test("should expose prometheus metrics when metrics endpoint is called") {
            // when
            val response = mockMvc.get("/metrics").andReturn().response

            // then
            response.status shouldBe 200
            response.contentAsString shouldContain "jvm_memory_used_bytes"
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            val workDir = Files.createTempDirectory("filebrowser-observability")
            registry.add("application.path") { workDir.toString() }
            TestTokens.register(registry)
        }
    }
}
