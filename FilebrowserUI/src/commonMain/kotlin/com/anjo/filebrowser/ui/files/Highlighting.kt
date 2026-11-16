package com.anjo.filebrowser.ui.files

import com.anjo.filebrowser.ui.model.Entry
import com.anjo.filebrowser.ui.model.OpenToken
import com.anjo.filebrowser.ui.model.PreviewLine
import com.anjo.filebrowser.ui.model.Syntax
import com.anjo.filebrowser.ui.model.Token
import com.anjo.filebrowser.ui.model.TokenKind
import com.anjo.filebrowser.ui.model.TokenizedLine

private const val LINE_END = "\n"
private const val MARKUP_COMMENT_START = "<!--"
private val MARKUP_COMMENT = OpenToken("-->", TokenKind.COMMENT)
private val INSIDE_TAG = OpenToken(">", TokenKind.TAG)

private val C_LIKE =
    Syntax.Code(
        keywords =
            (
                "abstract as async await auto bool boolean break byte case catch chan char class const constructor continue " +
                    "crate data def default defer delete do double dyn else enum explicit export extends extern false " +
                    "final finally float fn for friend from fun func function go goto if impl implements import in include " +
                    "init inline inner instanceof int interface internal is lateinit let long loop match mod move mut " +
                    "namespace native new null nullptr object open operator out override package private protected pub " +
                    "public range record return sealed select self short signed sizeof static struct super suspend switch " +
                    "synchronized template this throw throws trait transient true try typealias typedef typename typeof " +
                    "undefined union unsafe unsigned use using val var virtual void volatile when where while yield"
            ).split(' ').toSet(),
        lineComments = listOf("//"),
        blockComment = "/*" to "*/",
        multilineQuotes = listOf("\"\"\"", "`"),
    )

private val SCRIPT =
    Syntax.Code(
        keywords =
            (
                "False None True and as assert async await break case class continue def del do done echo elif else esac " +
                    "except exit export fi finally for from function global if import in is lambda local nonlocal not or " +
                    "pass raise readonly return self source then try until while with yield"
            ).split(' ').toSet(),
        lineComments = listOf("#"),
        multilineQuotes = listOf("\"\"\"", "'''"),
        commentAfterSpace = true,
    )

private val JSON = Syntax.Code(keywords = setOf("true", "false", "null"), quotes = "\"", keyFollower = Regex("""\s*:"""))

private val CONFIG =
    Syntax.Code(
        keywords = setOf("true", "false", "null", "yes", "no", "on", "off"),
        lineComments = listOf("#", ";"),
        multilineQuotes = listOf("\"\"\"", "'''"),
        wordChars = "_-.",
        keyFollower = Regex("""\s*(=|:(\s|$))"""),
        ignoreCase = true,
        commentAfterSpace = true,
    )

private val CSS =
    Syntax.Code(
        keywords =
            setOf(
                "@charset",
                "@container",
                "@font-face",
                "@import",
                "@keyframes",
                "@layer",
                "@media",
                "@page",
                "@supports",
                "important",
            ),
        blockComment = "/*" to "*/",
        wordChars = "_-@",
        keyFollower = Regex("""\s*:"""),
    )

private val SQL =
    Syntax.Code(
        keywords =
            (
                "add all alter and as asc begin between bigint boolean by cascade case check commit constraint create cross " +
                    "date default delete desc distinct drop else end exists false foreign from full grant group having if " +
                    "in index inner insert int integer into is join key left like limit not null numeric offset on or order " +
                    "outer primary references returning revoke right rollback select serial set table text then timestamp " +
                    "transaction true truncate union unique update values varchar view when where with"
            ).split(' ').toSet(),
        lineComments = listOf("--"),
        blockComment = "/*" to "*/",
        ignoreCase = true,
    )

private val SYNTAXES: Map<String, Syntax> =
    buildMap {
        listOf("kt", "kts", "java", "js", "ts", "go", "rs", "c", "h", "cpp").forEach { put(it, C_LIKE) }
        listOf("py", "sh").forEach { put(it, SCRIPT) }
        put("json", JSON)
        listOf("yaml", "yml", "toml", "ini", "conf", "properties").forEach { put(it, CONFIG) }
        listOf("xml", "html").forEach { put(it, Syntax.Markup) }
        put("css", CSS)
        put("sql", SQL)
    }

internal fun syntaxFor(entry: Entry): Syntax? = SYNTAXES[entry.extension]

internal class LineHighlighter(
    private val lines: List<PreviewLine>,
    private val syntax: Syntax,
) {
    private val opens = mutableListOf<OpenToken?>(null)

    fun tokens(index: Int): List<Token> = syntax.tokenize(lines[index].text, openAt(index)).tokens

    private fun openAt(index: Int): OpenToken? {
        while (opens.size <= index) {
            val previous = opens.lastIndex
            val open = syntax.tokenize(lines[previous].text, opens[previous]).open
            opens += open?.takeUnless { it.endsWithLine && !lines[previous + 1].continued }
        }
        return opens[index]
    }
}

private fun Syntax.tokenize(
    line: String,
    open: OpenToken?,
): TokenizedLine =
    when (this) {
        is Syntax.Code -> tokenizeCode(line, open)
        Syntax.Markup -> tokenizeMarkup(line, open)
    }

private fun Syntax.Code.tokenizeCode(
    line: String,
    open: OpenToken?,
): TokenizedLine {
    val tokens = mutableListOf<Token>()
    var pending = open
    var start = 0
    var i = 0
    while (true) {
        pending?.let { token ->
            val end = line.closing(i, token)
            if (end < 0) {
                tokens.addToken(start, line.length, token.kind)
                return TokenizedLine(tokens, token)
            }
            val key = token.kind == TokenKind.STRING && keyFollower?.matchesAt(line, end) == true
            tokens.addToken(start, end, if (key) TokenKind.KEY else token.kind)
            i = end
        }
        if (i >= line.length) return TokenizedLine(tokens, null)
        val opening = opening(line, i)
        pending = opening?.first
        start = i
        i = if (opening != null) i + opening.second else word(line, i, tokens)
    }
}

private fun Syntax.Code.opening(
    line: String,
    i: Int,
): Pair<OpenToken, Int>? {
    lineComments
        .firstOrNull { line.startsWith(it, i) && (!commentAfterSpace || i == 0 || line[i - 1].isWhitespace()) }
        ?.let { return OpenToken(LINE_END, TokenKind.COMMENT, endsWithLine = true) to it.length }
    blockComment?.takeIf { line.startsWith(it.first, i) }?.let { return OpenToken(it.second, TokenKind.COMMENT) to it.first.length }
    multilineQuotes.firstOrNull { line.startsWith(it, i) }?.let { return OpenToken(it, TokenKind.STRING) to it.length }
    return if (line[i] in quotes) OpenToken(line[i].toString(), TokenKind.STRING, endsWithLine = true) to 1 else null
}

private fun Syntax.Code.word(
    line: String,
    i: Int,
    tokens: MutableList<Token>,
): Int {
    val first = line[i]
    val number = first.isDigit()
    var end = i
    while (end < line.length && (line[end].isLetterOrDigit() || line[end] in wordChars || (number && line[end] == '.'))) end++
    if (end == i) return i + 1
    val word = line.substring(i, end)
    val kind =
        when {
            number -> TokenKind.NUMBER
            keyFollower?.matchesAt(line, end) == true -> TokenKind.KEY
            (if (ignoreCase) word.lowercase() else word) in keywords -> TokenKind.KEYWORD
            else -> null
        }
    kind?.let { tokens.addToken(i, end, it) }
    return end
}

private fun tokenizeMarkup(
    line: String,
    open: OpenToken?,
): TokenizedLine {
    val tokens = mutableListOf<Token>()
    var inTag = open == INSIDE_TAG
    var i = 0
    if (open != null && !inTag) {
        val end = line.closing(0, open)
        if (end < 0) {
            tokens.addToken(0, line.length, open.kind)
            return TokenizedLine(tokens, open)
        }
        tokens.addToken(0, end, open.kind)
        i = end
        inTag = open.kind == TokenKind.STRING
    }
    while (i < line.length) {
        val c = line[i]
        val opened =
            when {
                line.startsWith(MARKUP_COMMENT_START, i) -> MARKUP_COMMENT to MARKUP_COMMENT_START.length
                inTag && (c == '"' || c == '\'') -> OpenToken(c.toString(), TokenKind.STRING) to 1
                else -> null
            }
        if (opened != null) {
            val (token, length) = opened
            val end = line.closing(i + length, token)
            if (end < 0) {
                tokens.addToken(i, line.length, token.kind)
                return TokenizedLine(tokens, token)
            }
            tokens.addToken(i, end, token.kind)
            i = end
            continue
        }
        val end =
            when {
                inTag && line.startsWith("/>", i) -> i + 2
                inTag && c == '>' -> i + 1
                inTag && c.isNameChar() -> line.nameEnd(i)
                !inTag && c == '<' && i + 1 < line.length && (line[i + 1].isLetter() || line[i + 1] in "/?!") -> line.nameEnd(i + 2)
                else -> null
            }
        if (end == null) {
            i++
            continue
        }
        val attribute = inTag && c.isNameChar()
        tokens.addToken(i, end, if (attribute) TokenKind.ATTRIBUTE else TokenKind.TAG)
        inTag = attribute || c == '<'
        i = end
    }
    return TokenizedLine(tokens, INSIDE_TAG.takeIf { inTag })
}

private fun Char.isNameChar() = isLetterOrDigit() || this in "-_:."

private fun String.nameEnd(from: Int): Int {
    var end = minOf(from, length)
    while (end < length && this[end].isNameChar()) end++
    return end
}

private fun String.closing(
    from: Int,
    open: OpenToken,
): Int {
    if (open.kind != TokenKind.STRING) return indexOf(open.end, from).let { if (it < 0) it else it + open.end.length }
    var i = from
    while (i < length) {
        when {
            this[i] == '\\' -> i += 2
            startsWith(open.end, i) -> return i + open.end.length
            else -> i++
        }
    }
    return -1
}

private fun MutableList<Token>.addToken(
    start: Int,
    end: Int,
    kind: TokenKind,
) {
    val last = lastOrNull()
    if (last != null && last.end == start && last.kind == kind) set(lastIndex, last.copy(end = end)) else add(Token(start, end, kind))
}
