package com.anjo.filebrowser.ui.files

import com.anjo.filebrowser.ui.model.PreviewLine

internal const val PREVIEW_LINE_CHARS = 1000

internal fun previewLines(text: String): List<PreviewLine> =
    text
        .lines()
        .let { if (it.last().isEmpty()) it.dropLast(1) else it }
        .flatMap(::chunks)

private fun chunks(line: String): List<PreviewLine> =
    if (line.length <= PREVIEW_LINE_CHARS) {
        listOf(PreviewLine(line))
    } else {
        buildList {
            var start = 0
            while (start < line.length) {
                var end = minOf(start + PREVIEW_LINE_CHARS, line.length)
                if (end < line.length && line[end - 1].isHighSurrogate()) end--
                add(PreviewLine(line.substring(start, end), continued = start > 0))
                start = end
            }
        }
    }
