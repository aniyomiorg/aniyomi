package eu.kanade.domain.animehistory.model

import eu.kanade.domain.anime.model.AnimeCover
import java.util.Date

data class AnimeHistoryWithRelations(
    val id: Long,
    val episodeId: Long,
    val animeId: Long,
    val title: String,
    val episodeNumber: Float,
    val seenAt: Date?,
    val coverData: AnimeCover,
)
