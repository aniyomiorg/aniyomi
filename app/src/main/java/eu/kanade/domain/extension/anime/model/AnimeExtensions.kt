package eu.kanade.domain.extension.anime.model

import eu.kanade.tachiyomi.animeextension.model.AnimeExtension

data class AnimeExtensions(
    val updates: List<AnimeExtension.Installed>,
    val installed: List<AnimeExtension.Installed>,
    val available: List<AnimeExtension.Available>,
    val untrusted: List<AnimeExtension.Untrusted>,
)
