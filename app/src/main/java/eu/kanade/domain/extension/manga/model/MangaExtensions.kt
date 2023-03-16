package eu.kanade.domain.extension.manga.model

import eu.kanade.tachiyomi.extension.manga.model.MangaExtension

data class MangaExtensions(
    val updates: List<MangaExtension.Installed>,
    val installed: List<MangaExtension.Installed>,
    val available: List<MangaExtension.Available>,
    val untrusted: List<MangaExtension.Untrusted>,
)
