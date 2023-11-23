package eu.kanade.tachiyomi.extension.manga.model

sealed interface MangaLoadResult {
    data class Success(val extension: MangaExtension.Installed) : MangaLoadResult
    data class Untrusted(val extension: MangaExtension.Untrusted) : MangaLoadResult
    data object Error : MangaLoadResult
}
