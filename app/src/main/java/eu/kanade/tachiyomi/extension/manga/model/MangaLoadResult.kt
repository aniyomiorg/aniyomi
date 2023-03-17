package eu.kanade.tachiyomi.extension.manga.model

sealed class MangaLoadResult {
    class Success(val extension: MangaExtension.Installed) : MangaLoadResult()
    class Untrusted(val extension: MangaExtension.Untrusted) : MangaLoadResult()
    object Error : MangaLoadResult()
}
