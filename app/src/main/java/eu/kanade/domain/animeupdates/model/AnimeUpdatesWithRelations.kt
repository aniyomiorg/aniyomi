package eu.kanade.domain.animeupdates.model

import eu.kanade.domain.anime.model.AnimeCover

data class AnimeUpdatesWithRelations(
    val animeId: Long,
    val animeTitle: String,
    val episodeId: Long,
    val episodeName: String,
    val scanlator: String?,
    val seen: Boolean,
    val bookmark: Boolean,
    val sourceId: Long,
    val dateFetch: Long,
    val coverData: AnimeCover,
)
