package aniyomi.domain.anime

import tachiyomi.domain.entries.anime.model.Anime

data class SeasonAnime(
    val anime: Anime,
    val totalCount: Long,
    val seenCount: Long,
    val bookmarkCount: Long,
    val latestUpload: Long,
    val fetchedAt: Long,
    val lastSeen: Long,
) {
    val id: Long = anime.id

    val unseenCount
        get() = totalCount - seenCount

    val hasBookmarks
        get() = bookmarkCount > 0

    val hasStarted = seenCount > 0
}
