package com.anjo.filebrowser.ui.files

import com.anjo.filebrowser.ui.file
import com.anjo.filebrowser.ui.folder
import com.anjo.filebrowser.ui.model.Entry
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe

private data class KindCase(
    val entry: Entry,
    val expected: PreviewKind?,
)

private fun typed(
    path: String,
    contentType: String,
) = file(path).copy(contentType = contentType)

class PreviewKindTest :
    FunSpec({
        context("should pick the preview kind by extension first and by content type second") {
            withTests(
                nameFn = { listOfNotNull(it.entry.path, it.entry.contentType).joinToString(" ") },
                KindCase(file("/notes.TXT"), PreviewKind.TEXT),
                KindCase(file("/src/Main.kt"), PreviewKind.TEXT),
                KindCase(file("/config.yml"), PreviewKind.TEXT),
                KindCase(typed("/app.ts", "video/mp2t"), PreviewKind.TEXT),
                KindCase(file("/photo.JPEG"), PreviewKind.IMAGE),
                KindCase(file("/animation.gif"), PreviewKind.IMAGE),
                KindCase(typed("/README", "text/plain; charset=UTF-8"), PreviewKind.TEXT),
                KindCase(typed("/data", "application/json"), PreviewKind.TEXT),
                KindCase(typed("/picture", "image/webp"), PreviewKind.IMAGE),
                KindCase(typed("/logo.svg", "image/svg+xml"), null),
                KindCase(typed("/manual.pdf", "application/pdf"), null),
                KindCase(typed("/song.mp3", "audio/mpeg"), null),
                KindCase(typed("/clip.mp4", "video/mp4"), null),
                KindCase(file("/archive.zip"), null),
                KindCase(folder("/Docs.txt"), null),
            ) { case ->
                // when
                val kind = previewKind(case.entry)

                // then
                kind shouldBe case.expected
            }
        }
    })
