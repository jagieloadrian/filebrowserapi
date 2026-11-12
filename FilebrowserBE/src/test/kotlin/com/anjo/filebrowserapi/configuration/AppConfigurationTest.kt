package com.anjo.filebrowserapi.configuration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AppConfigurationTest {
    @Test
    fun `should default to working directory when path is not set`() {
        // when
        val result = AppConfiguration()

        // then
        result.path shouldBe System.getProperty("user.dir")
    }

    @Test
    fun `should reject configuration when path is blank`() {
        // when
        val result = shouldThrow<IllegalArgumentException> { AppConfiguration(" ") }

        // then
        result.message shouldBe "application.path must not be blank"
    }
}
