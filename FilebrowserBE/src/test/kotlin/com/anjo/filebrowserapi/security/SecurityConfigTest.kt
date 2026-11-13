package com.anjo.filebrowserapi.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

class SecurityConfigTest :
    FunSpec({
        test("should refuse to start when no audience is configured") {
            // when
            val result = shouldThrow<IllegalArgumentException> { SecurityConfig().jwtAuthenticationConverter(emptyList()) }

            // then
            result.message shouldContain "audiences"
        }
    })
