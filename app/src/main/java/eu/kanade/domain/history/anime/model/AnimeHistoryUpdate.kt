package eu.kanade.domain.history.anime.model

import java.util.Date

data class AnimeHistoryUpdate(
    val episodeId: Long,
    val seenAt: Date,
)
