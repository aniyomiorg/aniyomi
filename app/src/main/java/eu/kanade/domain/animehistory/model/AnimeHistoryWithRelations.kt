package eu.kanade.domain.animehistory.model

import java.util.Date

data class AnimeHistoryWithRelations(
    val id: Long,
    val episodeId: Long,
    val animeId: Long,
    val title: String,
    val thumbnailUrl: String,
    val episodeNumber: Float,
    val seenAt: Date?
)
