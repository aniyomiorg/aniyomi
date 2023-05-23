package tachiyomi.domain.history.anime.model

import tachiyomi.domain.entries.anime.model.AnimeCover
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
