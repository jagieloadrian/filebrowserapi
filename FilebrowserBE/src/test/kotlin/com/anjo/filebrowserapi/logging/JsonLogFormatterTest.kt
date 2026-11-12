package com.anjo.filebrowserapi.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import com.anjo.filebrowserapi.TestObjects.TEST_MESSAGE
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Instant
import kotlin.test.Test

class JsonLogFormatterTest {
    private val formatter = JsonLogFormatter()
    private val logger = LoggerContext().getLogger("com.anjo.Test")

    @Test
    fun `should write timestamp level message logger and traceId when event has trace`() {
        // given
        val event = event(mapOf("traceId" to TRACE_ID))

        // when
        val result = jacksonObjectMapper().readValue<Map<String, String>>(formatter.format(event))

        // then
        result shouldContainExactly
            mapOf(
                "timestamp" to TIMESTAMP.toString(),
                "level" to "INFO",
                "message" to TEST_MESSAGE,
                "logger" to "com.anjo.Test",
                "traceId" to TRACE_ID,
            )
    }

    @Test
    fun `should omit traceId when event has no trace`() {
        // given
        val event = event(emptyMap())

        // when
        val result = jacksonObjectMapper().readValue<Map<String, String>>(formatter.format(event))

        // then
        result.keys shouldBe setOf("timestamp", "level", "message", "logger")
    }

    @Test
    fun `should end each record with a newline when formatting`() {
        // when
        val result = formatter.format(event(emptyMap()))

        // then
        result.last() shouldBe '\n'
    }

    private fun event(mdc: Map<String, String>) =
        LoggingEvent("fqcn", logger, Level.INFO, TEST_MESSAGE, null, null).apply {
            instant = TIMESTAMP
            mdcPropertyMap = mdc
        }

    private companion object {
        const val TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736"
        val TIMESTAMP: Instant = Instant.parse("2026-11-12T10:15:30Z")
    }
}
