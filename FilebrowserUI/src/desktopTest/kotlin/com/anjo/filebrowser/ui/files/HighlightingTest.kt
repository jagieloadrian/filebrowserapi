package com.anjo.filebrowser.ui.files

import com.anjo.filebrowser.ui.file
import com.anjo.filebrowser.ui.model.TokenKind
import com.anjo.filebrowser.ui.model.TokenKind.ATTRIBUTE
import com.anjo.filebrowser.ui.model.TokenKind.COMMENT
import com.anjo.filebrowser.ui.model.TokenKind.KEY
import com.anjo.filebrowser.ui.model.TokenKind.KEYWORD
import com.anjo.filebrowser.ui.model.TokenKind.NUMBER
import com.anjo.filebrowser.ui.model.TokenKind.STRING
import com.anjo.filebrowser.ui.model.TokenKind.TAG
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe

private data class HighlightCase(
    val name: String,
    val file: String,
    val text: String,
    val tokens: List<Pair<String, TokenKind>>,
)

private fun highlight(
    file: String,
    text: String,
): List<Pair<String, TokenKind>> {
    val lines = previewLines(text)
    val highlighter = LineHighlighter(lines, checkNotNull(syntaxFor(file(file))) { "No syntax for $file" })
    return lines.indices.flatMap { index ->
        highlighter.tokens(index).map { lines[index].text.substring(it.start, it.end) to it.kind }
    }
}

private val longValue = "a".repeat(PREVIEW_LINE_CHARS + 500)

class HighlightingTest :
    FunSpec({
        context("should find keywords, strings, comments and numbers in each family") {
            withTests(
                nameFn = { it.name },
                HighlightCase(
                    "C-like",
                    "/Main.kt",
                    "val s = \"a\\\"b\" // note\n/* one\ntwo */ return 42",
                    listOf(
                        "val" to KEYWORD,
                        "\"a\\\"b\"" to STRING,
                        "// note" to COMMENT,
                        "/* one" to COMMENT,
                        "two */" to COMMENT,
                        "return" to KEYWORD,
                        "42" to NUMBER,
                    ),
                ),
                HighlightCase(
                    "C-like string left open at the end of a line",
                    "/app.js",
                    "let s = \"open\nlet n = 1.5",
                    listOf("let" to KEYWORD, "\"open" to STRING, "let" to KEYWORD, "1.5" to NUMBER),
                ),
                HighlightCase(
                    "python",
                    "/tool.py",
                    "def f(): return 'it\\'s'  # note\n\"\"\"one\ntwo\"\"\" 0x1F",
                    listOf(
                        "def" to KEYWORD,
                        "return" to KEYWORD,
                        "'it\\'s'" to STRING,
                        "# note" to COMMENT,
                        "\"\"\"one" to STRING,
                        "two\"\"\"" to STRING,
                        "0x1F" to NUMBER,
                    ),
                ),
                HighlightCase(
                    "shell",
                    "/run.sh",
                    "if [ \$# -gt 2 ]; then echo \"a\\\"b\" # note",
                    listOf(
                        "if" to KEYWORD,
                        "2" to NUMBER,
                        "then" to KEYWORD,
                        "echo" to KEYWORD,
                        "\"a\\\"b\"" to STRING,
                        "# note" to COMMENT,
                    ),
                ),
                HighlightCase(
                    "JSON",
                    "/data.json",
                    "{\"key\": \"a\\\"b\", \"n\": -1.5e3, \"ok\": true}",
                    listOf(
                        "\"key\"" to KEY,
                        "\"a\\\"b\"" to STRING,
                        "\"n\"" to KEY,
                        "1.5e3" to NUMBER,
                        "\"ok\"" to KEY,
                        "true" to KEYWORD,
                    ),
                ),
                HighlightCase(
                    "JSON string across a long line chunk",
                    "/data.json",
                    "{\"k\": \"$longValue\", \"n\": 1}",
                    listOf(
                        "\"k\"" to KEY,
                        "\"" + longValue.take(PREVIEW_LINE_CHARS - 7) to STRING,
                        longValue.drop(PREVIEW_LINE_CHARS - 7) + "\"" to STRING,
                        "\"n\"" to KEY,
                        "1" to NUMBER,
                    ),
                ),
                HighlightCase(
                    "YAML",
                    "/config.yaml",
                    "name: \"a\\\"b\" # note\nurl: http://host:80\n- debug: true\nport: 8080",
                    listOf(
                        "name" to KEY,
                        "\"a\\\"b\"" to STRING,
                        "# note" to COMMENT,
                        "url" to KEY,
                        "80" to NUMBER,
                        "debug" to KEY,
                        "true" to KEYWORD,
                        "port" to KEY,
                        "8080" to NUMBER,
                    ),
                ),
                HighlightCase(
                    "TOML",
                    "/settings.toml",
                    "text = \"\"\"one\ntwo\"\"\"\nserver.port = 80 ; note\nenabled = FALSE",
                    listOf(
                        "text" to KEY,
                        "\"\"\"one" to STRING,
                        "two\"\"\"" to STRING,
                        "server.port" to KEY,
                        "80" to NUMBER,
                        "; note" to COMMENT,
                        "enabled" to KEY,
                        "FALSE" to KEYWORD,
                    ),
                ),
                HighlightCase(
                    "XML",
                    "/page.html",
                    "<!-- one\ntwo --> <a href=\"x\\\"y\"\n  id='z'>1</a> <br/>",
                    listOf(
                        "<!-- one" to COMMENT,
                        "two -->" to COMMENT,
                        "<a" to TAG,
                        "href" to ATTRIBUTE,
                        "\"x\\\"y\"" to STRING,
                        "id" to ATTRIBUTE,
                        "'z'" to STRING,
                        ">" to TAG,
                        "</a>" to TAG,
                        "<br/>" to TAG,
                    ),
                ),
                HighlightCase(
                    "CSS",
                    "/style.css",
                    "/* one\ntwo */ a { content: \"a\\\"b\"; margin: 10px !important; }\n@media print {}",
                    listOf(
                        "/* one" to COMMENT,
                        "two */" to COMMENT,
                        "content" to KEY,
                        "\"a\\\"b\"" to STRING,
                        "margin" to KEY,
                        "10px" to NUMBER,
                        "important" to KEYWORD,
                        "@media" to KEYWORD,
                    ),
                ),
                HighlightCase(
                    "SQL",
                    "/query.sql",
                    "SELECT 'it''s', 42 -- note\n/* one\ntwo */ from t",
                    listOf(
                        "SELECT" to KEYWORD,
                        "'it''s'" to STRING,
                        "42" to NUMBER,
                        "-- note" to COMMENT,
                        "/* one" to COMMENT,
                        "two */" to COMMENT,
                        "from" to KEYWORD,
                    ),
                ),
            ) { case ->
                // when
                val tokens = highlight(case.file, case.text)

                // then
                tokens shouldBe case.tokens
            }
        }

        context("should leave plain text files unhighlighted") {
            withTests("/notes.txt", "/README.md", "/app.log", "/data.csv", "/table.tsv", "/README", "/archive.unknown") { path ->
                // when
                val syntax = syntaxFor(file(path))

                // then
                syntax shouldBe null
            }
        }
    })
