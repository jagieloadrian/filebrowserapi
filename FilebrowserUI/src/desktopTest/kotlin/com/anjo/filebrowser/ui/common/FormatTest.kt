package com.anjo.filebrowser.ui.common

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import java.util.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
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

        context("formatTime") {
            withTests(
                nameFn = { "should show ${it.first} at ${it.second} as ${it.third}" },
                Triple("2026-11-13T10:05:30.123Z", Duration.ZERO, "2026-11-13\u00A010:05"),
                Triple("2026-11-13T10:05:00Z", 1.hours, "2026-11-13\u00A011:05"),
                Triple("2026-11-13T23:30:00Z", 2.hours, "2026-11-14\u00A001:30"),
                Triple("2026-11-13T02:00:00Z", (-5).hours, "2026-11-12\u00A021:00"),
                Triple("2026-11-13T10:00:00Z", 5.hours + 30.minutes, "2026-11-13\u00A015:30"),
            ) { (instant, offset, expected) ->
                formatTime(Instant.parse(instant), offset) shouldBe expected
            }
        }

        context("localOffset") {
            withTests(
                nameFn = { "should find ${it.third} in ${it.first} at ${it.second}" },
                Triple("Europe/Berlin", "2026-01-15T12:00:00Z", 1.hours),
                Triple("Europe/Berlin", "2026-07-15T12:00:00Z", 2.hours),
                Triple("America/New_York", "2026-01-15T12:00:00Z", (-5).hours),
                Triple("UTC", "2026-07-15T12:00:00Z", Duration.ZERO),
            ) { (zone, instant, expected) ->
                // given
                val system = TimeZone.getDefault()
                TimeZone.setDefault(TimeZone.getTimeZone(zone))

                // when
                val offset =
                    try {
                        localOffset(Instant.parse(instant))
                    } finally {
                        TimeZone.setDefault(system)
                    }

                // then
                offset shouldBe expected
            }
        }

        test("should keep every size and the date and time on one line") {
            listOf(742L, 1_153_434L, 2_684_354_560L).forEach { formatSize(it) shouldNotContain " " }
            formatTime(Instant.parse("2026-11-17T15:32:00Z")) shouldNotContain " "
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
