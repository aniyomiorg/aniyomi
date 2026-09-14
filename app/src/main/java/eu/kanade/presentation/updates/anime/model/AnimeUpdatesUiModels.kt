package eu.kanade.presentation.updates.anime.model

import tachiyomi.domain.entries.anime.model.AnimeCover

class AnimeUpdatesUiModels {
    data class Anime(
        val animeId: Long,
        val animeTitle: String,
        val coverData: AnimeCover,
        val subText: String,
    )
}
