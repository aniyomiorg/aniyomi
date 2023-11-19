package tachiyomi.domain.history.anime.model

import java.util.Date
import tachiyomi.domain.entries.anime.model.AnimeCover

data class AnimeHistoryWithRelations(
    val id: Long,
    val episodeId: Long,
    val animeId: Long,
    val title: String,
    val episodeNumber: Double,
    val seenAt: Date?,
    val coverData: AnimeCover,
)
