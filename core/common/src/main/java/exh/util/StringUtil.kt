package exh.util

import java.util.Locale

fun Collection<String>.trimAll() = map { it.trim() }
fun Collection<String>.dropBlank() = filter { it.isNotBlank() }
fun Collection<String>.dropEmpty() = filter { it.isNotEmpty() }

private val articleRegex by lazy { "^(an|a|the) ".toRegex(RegexOption.IGNORE_CASE) }

fun String.removeArticles(): String {
    return replace(articleRegex, "")
}

fun String.trimOrNull() = trim().nullIfBlank()

fun String.nullIfBlank(): String? = ifBlank { null }

fun String.capitalize(locale: Locale = Locale.getDefault()) =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
