package eu.kanade.domain.animehistory.model

import java.util.Date

data class AnimeHistoryUpdate(
    val episodeId: Long,
    val seenAt: Date,
)
