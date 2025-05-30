package eu.kanade.presentation.more.settings.screen.player.editor.codeeditor

import androidx.compose.ui.graphics.Color

val githubTheme = SyntaxColors(
    keyword = Color(0xFFFF7B72),
    literal = Color(0xFF79C0FF),
    comment = Color(0xFF9198A1),
    string = Color(0xFFA5D6FF),
    definition = Color(0xFFD2A8FF),
    key = Color(0xFF7EE787),
    value = Color(0xFFD29922),
)

// From https://github.com/Qawaz/compose-code-editor
class SyntaxColors(
    val keyword: Color,
    val literal: Color,
    val comment: Color,
    val string: Color,
    val definition: Color,
    val key: Color,
    val value: Color,
)

// Some rules taken from https://github.com/Qawaz/compose-code-editor
fun luaHighlight(syntaxColors: SyntaxColors) = Highlight(
    rules = listOf(
        // Operators
        HighlightType.Full(
            regex = Regex("""(?<![=<>~:/])(?:==|~=|<=|>=|\/\/|\.\.|[=+\-*/%^#<>])"""),
            color = syntaxColors.keyword,
        ),

        // Invocation with `:`
        HighlightType.Full(
            regex = Regex("""\b([a-zA-Z_]\w*)(?=\s*:)"""),
            color = syntaxColors.definition,
        ),

        // Function calls
        HighlightType.Groups(
            regex = Regex("""([a-zA-Z_]\w*)(?=\s*\()"""),
            colors = listOf(syntaxColors.literal),
        ),

        // Function definition
        HighlightType.Groups(
            regex = Regex("""function\s+([a-zA-Z_][\w.]*)(?=\s*\()"""),
            colors = listOf(syntaxColors.definition),
        ),

        // Keywords
        HighlightType.Groups(
            regex = Regex(
                """\b(and|break|do|else|elseif|end|for|function|if|in|local|not|or|repeat|return|then|until|while)\b""",
            ),
            colors = listOf(syntaxColors.keyword),
        ),

        // True/false/nil
        HighlightType.Groups(
            regex = Regex("""\b(true|false|nil)\b"""),
            colors = listOf(syntaxColors.literal),
        ),

        // A number is a hex integer literal, a decimal real literal, or in
        // scientific notation.
        HighlightType.Full(
            regex = Regex(
                """(?<!\w)[+-]?(?:0x[\da-f]+|(?:(?:\.\d+|\d+(?:\.\d*)?)(?:e[+\-]?\d+)?))""",
                RegexOption.IGNORE_CASE,
            ),
            color = syntaxColors.literal,
        ),

        // A double or single quoted, possibly multi-line, string.
        HighlightType.Full(
            regex = Regex("""(?<!--[^\n]{0,120})("(?:[^"\\]|\\[\s\S])*"|\'(?:[^'\\]|\\[\s\S])*\')"""),
            color = syntaxColors.string,
            isString = true,
        ),

        // Single line comment
        HighlightType.Full(
            regex = Regex("""--.*${'$'}""", RegexOption.MULTILINE),
            color = syntaxColors.comment,
        ),

        // Multi-line comment
        HighlightType.Full(
            regex = Regex("""--\[(=*)\[.*?--\]\1\]""", RegexOption.DOT_MATCHES_ALL),
            color = syntaxColors.comment,
        ),
    ),
)

fun confHighlight(syntaxColors: SyntaxColors) = Highlight(
    rules = listOf(
        // Key
        HighlightType.Full(
            regex = Regex("""^\s*[\w.-]+(?=\s*=)""", RegexOption.MULTILINE),
            color = syntaxColors.literal,
        ),

        // Value
        HighlightType.Full(
            regex = Regex("""(?<==)[ \t]*[\w ~,./;:-]+"""),
            color = syntaxColors.literal,
        ),

        // Yes/No
        HighlightType.Groups(
            regex = Regex("""(?<![\w-])(yes|no)(?![\w-])"""),
            colors = listOf(syntaxColors.value),
        ),

        // Numbers
        HighlightType.Full(
            regex = Regex("""\b\d+(\.\d+)?\b"""),
            color = syntaxColors.value,
        ),

        // String
        HighlightType.Full(
            regex = Regex("""(?<!#[^\n]{0,120})("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*')"""),
            color = syntaxColors.key,
            isString = true,
        ),

        // Comment
        HighlightType.Full(
            regex = Regex("""#.*"""),
            color = syntaxColors.comment,
        ),
    ),
)
