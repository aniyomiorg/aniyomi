package eu.kanade.tachiyomi.extension.anime.model

sealed class AnimeLoadResult {
    data class Success(val extension: AnimeExtension.Installed) : AnimeLoadResult()
    data class Untrusted(val extension: AnimeExtension.Untrusted) : AnimeLoadResult()
    data object Error : AnimeLoadResult()
}
