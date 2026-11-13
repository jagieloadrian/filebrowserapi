package com.anjo.filebrowserapi.configuration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AppConfigurationTest :
    FunSpec({
        test("should default to working directory when path is not set") {
            // when
            val result = AppConfiguration()

            // then
            result.path shouldBe System.getProperty("user.dir")
        }

        test("should reject configuration when path is blank") {
            // when
            val result = shouldThrow<IllegalArgumentException> { AppConfiguration(" ") }

            // then
            result.message shouldBe "application.path must not be blank"
        }
    })
