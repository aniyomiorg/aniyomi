package eu.kanade.presentation.more.settings.screen.player.editor.codeeditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle

sealed class HighlightType(open val regex: Regex, open val isString: Boolean) {
    data class Full(
        override val regex: Regex,
        override val isString: Boolean = false,
        val color: Color,
    ) : HighlightType(regex, isString)

    data class Groups(
        override val regex: Regex,
        override val isString: Boolean = false,
        val colors: List<Color>,
    ) : HighlightType(regex, isString)
}

data class Highlight(
    val rules: List<HighlightType>,
)

// From https://github.com/NeoUtils/Highlight
fun Highlight.toAnnotatedString(text: String): AnnotatedString {
    val spanStyles = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    val stringRanges = mutableListOf<IntRange>()

    for (rule in rules) {
        for (result in rule.regex.findAll(text)) {
            for ((index, group) in result.groups.withIndex()) {
                if (group == null) continue
                if (rule is HighlightType.Groups && index == 0) continue
                if (stringRanges.any { it.contains(group.range.first) }) continue

                val color = when (rule) {
                    is HighlightType.Full -> rule.color
                    is HighlightType.Groups -> rule.colors[index - 1]
                }

                spanStyles.add(
                    AnnotatedString.Range(
                        item = SpanStyle(color = color),
                        start = group.range.first,
                        end = group.range.last + 1,
                    ),
                )

                if (rule.isString) {
                    stringRanges.add(group.range)
                }
            }
        }
    }

    return AnnotatedString(
        text = text,
        spanStyles = spanStyles,
    )
}
