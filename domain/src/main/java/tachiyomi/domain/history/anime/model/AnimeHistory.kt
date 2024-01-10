package tachiyomi.domain.history.anime.model

import java.util.Date

data class AnimeHistory(
    val id: Long,
    val episodeId: Long,
    val seenAt: Date?,
) {
    companion object {
        fun create() = AnimeHistory(
            id = -1L,
            episodeId = -1L,
            seenAt = null,
        )
    }
}
