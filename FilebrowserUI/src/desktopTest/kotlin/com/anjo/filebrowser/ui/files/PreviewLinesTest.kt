package com.anjo.filebrowser.ui.files

import com.anjo.filebrowser.ui.model.PreviewLine
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe

private const val EMOJI = "😀"

private data class LinesCase(
    val name: String,
    val text: String,
    val lines: List<PreviewLine>,
)

class PreviewLinesTest :
    FunSpec({
        context("should split the text into lines and long lines into bounded chunks") {
            withTests(
                nameFn = { it.name },
                LinesCase("empty text has no lines", "", emptyList()),
                LinesCase("CRLF, LF and CR end lines", "a\r\nb\nc\rd", listOf("a", "b", "c", "d").map(::PreviewLine)),
                LinesCase("a trailing newline adds no line", "a\n", listOf(PreviewLine("a"))),
                LinesCase("empty lines stay", "a\n\nb\n\n", listOf("a", "", "b", "").map(::PreviewLine)),
                LinesCase(
                    "a long line is chunked",
                    "x".repeat(PREVIEW_LINE_CHARS * 2 + 5),
                    listOf(
                        PreviewLine("x".repeat(PREVIEW_LINE_CHARS)),
                        PreviewLine("x".repeat(PREVIEW_LINE_CHARS), continued = true),
                        PreviewLine("xxxxx", continued = true),
                    ),
                ),
                LinesCase(
                    "a chunk never splits a character",
                    "x".repeat(PREVIEW_LINE_CHARS - 1) + EMOJI + "y",
                    listOf(PreviewLine("x".repeat(PREVIEW_LINE_CHARS - 1)), PreviewLine("${EMOJI}y", continued = true)),
                ),
            ) { case ->
                // when
                val lines = previewLines(case.text)

                // then
                lines shouldBe case.lines
            }
        }
    })
