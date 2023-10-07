package tachiyomi.domain.history.anime.model

import java.util.Date

data class AnimeHistory(
    val id: Long,
    val episodeId: Long,
    val seenAt: Date?,
)
