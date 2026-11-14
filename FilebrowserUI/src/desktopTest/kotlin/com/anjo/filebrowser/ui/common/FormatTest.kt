package com.anjo.filebrowser.ui.common

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import kotlin.time.Instant

class FormatTest :
    FunSpec({
        context("formatSize") {
            withTests(
                nameFn = { "should show ${it.first} bytes as ${it.second}" },
                0L to "0 B",
                1023L to "1023 B",
                1024L to "1.0 KB",
                1536L to "1.5 KB",
                5L * 1024 * 1024 * 1024 to "5.0 GB",
            ) { (bytes, expected) ->
                formatSize(bytes) shouldBe expected
            }
        }

        test("should show the time in minutes and UTC") {
            formatTime(Instant.parse("2026-11-13T10:05:30.123Z")) shouldBe "2026-11-13 10:05 UTC"
        }

        test("should name the root folder Home") {
            folderLabel("") shouldBe "Home"
            folderLabel("/Docs") shouldBe "/Docs"
        }
    })
