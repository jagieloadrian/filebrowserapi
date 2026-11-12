package com.anjo.filebrowserapi.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class SecurityConfigTest {
    @Test
    fun `should refuse to start when no audience is configured`() {
        // when
        val result = shouldThrow<IllegalArgumentException> { SecurityConfig().jwtAuthenticationConverter(emptyList()) }

        // then
        result.message shouldContain "audiences"
    }
}
