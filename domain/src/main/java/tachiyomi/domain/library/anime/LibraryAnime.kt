package tachiyomi.domain.library.anime

import tachiyomi.domain.entries.anime.model.Anime

data class LibraryAnime(
    val anime: Anime,
    val category: Long,
    val totalEpisodes: Long,
    val seenCount: Long,
    val bookmarkCount: Long,
    val latestUpload: Long,
    val episodeFetchedAt: Long,
    val lastSeen: Long,
) {
    val id: Long = anime.id

    val unseenCount
        get() = totalEpisodes - seenCount

    val hasBookmarks
        get() = bookmarkCount > 0

    val hasStarted = seenCount > 0
}
