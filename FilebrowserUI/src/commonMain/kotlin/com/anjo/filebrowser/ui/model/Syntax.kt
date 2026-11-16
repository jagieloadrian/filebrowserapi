package com.anjo.filebrowser.ui.model

enum class TokenKind { KEYWORD, STRING, COMMENT, NUMBER, KEY, TAG, ATTRIBUTE }

data class Token(
    val start: Int,
    val end: Int,
    val kind: TokenKind,
)

data class OpenToken(
    val end: String,
    val kind: TokenKind,
    val endsWithLine: Boolean = false,
)

data class TokenizedLine(
    val tokens: List<Token>,
    val open: OpenToken?,
)

sealed interface Syntax {
    data class Code(
        val keywords: Set<String> = emptySet(),
        val lineComments: List<String> = emptyList(),
        val blockComment: Pair<String, String>? = null,
        val quotes: String = "\"'",
        val multilineQuotes: List<String> = emptyList(),
        val wordChars: String = "_",
        val keyFollower: Regex? = null,
        val ignoreCase: Boolean = false,
        val commentAfterSpace: Boolean = false,
    ) : Syntax

    data object Markup : Syntax
}
