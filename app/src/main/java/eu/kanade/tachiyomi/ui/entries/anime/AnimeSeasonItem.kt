package eu.kanade.tachiyomi.ui.entries.anime

import aniyomi.domain.anime.SeasonAnime

data class AnimeSeasonItem(
    val seasonAnime: SeasonAnime,
    val downloadCount: Long = -1L,
    val unseenCount: Long = -1L,
    val isLocal: Boolean = false,
    val sourceLanguage: String = "",
    val showContinueOverlay: Boolean = false,
)
