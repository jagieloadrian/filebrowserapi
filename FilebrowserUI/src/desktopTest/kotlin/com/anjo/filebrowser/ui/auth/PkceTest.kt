package com.anjo.filebrowser.ui.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch

class PkceTest :
    FunSpec({
        test("should derive the S256 challenge from the RFC 7636 appendix B verifier") {
            // when
            val challenge = codeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")

            // then
            challenge shouldBe "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
        }

        test("should create a fresh verifier of 64 unreserved characters and a fresh state") {
            // when
            val verifiers = listOf(newVerifier(), newVerifier())
            val states = listOf(newState(), newState())

            // then
            verifiers[0] shouldMatch Regex("[0-9a-f]{64}")
            verifiers[0] shouldNotBe verifiers[1]
            states[0] shouldMatch Regex("[0-9a-f]{32}")
            states[0] shouldNotBe states[1]
        }
    })
