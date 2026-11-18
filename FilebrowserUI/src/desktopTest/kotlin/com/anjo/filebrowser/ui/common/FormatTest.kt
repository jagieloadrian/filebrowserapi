package com.anjo.filebrowser.ui.common

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import kotlin.time.Instant

class FormatTest :
    FunSpec({
        context("formatSize") {
            withTests(
                nameFn = { "should show ${it.first} bytes as ${it.second}" },
                0L to "0\u00A0B",
                1023L to "1023\u00A0B",
                1024L to "1.0\u00A0KB",
                1536L to "1.5\u00A0KB",
                5L * 1024 * 1024 * 1024 to "5.0\u00A0GB",
            ) { (bytes, expected) ->
                formatSize(bytes) shouldBe expected
            }
        }

        test("should show the time in minutes and UTC") {
            formatTime(Instant.parse("2026-11-13T10:05:30.123Z")) shouldBe "2026-11-13 10:05\u00A0UTC"
        }

        test("should keep every size and the time of day with its unit on one line") {
            listOf(742L, 1_153_434L, 2_684_354_560L).forEach { formatSize(it) shouldNotContain " " }
            formatTime(Instant.parse("2026-11-17T15:32:00Z")) shouldEndWith "15:32\u00A0UTC"
        }

        context("fileType") {
            withTests(
                nameFn = { "should name the type of ${it.first} ${it.second}" },
                "report.pdf" to "PDF file",
                "archive.tar.gz" to "GZ file",
                "README" to "File",
                ".bashrc" to "File",
            ) { (name, expected) ->
                fileType(name) shouldBe expected
            }
        }

        test("should name the root folder Home") {
            folderLabel("") shouldBe "Home"
            folderLabel("/Docs") shouldBe "/Docs"
        }
    })
