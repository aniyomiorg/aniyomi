package aniyomi.util

fun <C : Collection<R>, R> C.nullIfEmpty() = ifEmpty { null }
