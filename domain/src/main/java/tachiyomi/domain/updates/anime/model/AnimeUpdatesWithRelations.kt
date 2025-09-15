package tachiyomi.domain.updates.anime.model

import tachiyomi.domain.entries.anime.model.AnimeCover

data class AnimeUpdatesWithRelations(
    val animeId: Long,
    val animeTitle: String,
    val episodeId: Long,
    val episodeName: String,
    val scanlator: String?,
    val seen: Boolean,
    val bookmark: Boolean,
    val fillermark: Boolean,
    val lastSecondSeen: Long,
    val totalSeconds: Long,
    val sourceId: Long,
    val dateFetch: Long,
    val coverData: AnimeCover,
)
