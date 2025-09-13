package aniyomi.domain.anime

import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.anime.LibraryAnime

data class SeasonAnime(
    val anime: Anime,
    val totalCount: Long,
    val seenCount: Long,
    val bookmarkCount: Long,
    val fillermarkCount: Long,
    val latestUpload: Long,
    val fetchedAt: Long,
    val lastSeen: Long,
) {
    val id: Long = anime.id

    val seen
        get() = totalCount == seenCount

    val unseenCount
        get() = totalCount - seenCount

    val hasStarted = seenCount > 0

    val hasBookmarks
        get() = bookmarkCount > 0

    val hasFillermarks
        get() = fillermarkCount > 0

    fun toLibraryAnime(): LibraryAnime {
        return LibraryAnime(
            anime = anime,
            category = -1L,
            totalCount = totalCount,
            seenCount = seenCount,
            bookmarkCount = bookmarkCount,
            fillermarkCount = fillermarkCount,
            latestUpload = latestUpload,
            episodeFetchedAt = fetchedAt,
            lastSeen = lastSeen,
        )
    }
}
