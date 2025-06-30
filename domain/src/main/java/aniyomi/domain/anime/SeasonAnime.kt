package aniyomi.domain.anime

import tachiyomi.domain.entries.anime.model.Anime

data class SeasonAnime(
    val anime: Anime,
    val totalEpisodes: Long,
    val seenEpisodesCount: Long,
    val bookmarkCount: Long,
    val latestUpload: Long,
    val fetchedAt: Long,
    val lastSeen: Long,
) {
    val id: Long = anime.id

    val unseenEpisodeCount
        get() = totalEpisodes - seenEpisodesCount

    val hasBookmarks
        get() = bookmarkCount > 0

    val hasStarted = seenEpisodesCount > 0
}
