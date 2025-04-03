package tachiyomi.data.entries.anime

import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.library.anime.LibraryAnime

object AnimeMapper {
    fun mapAnime(
        id: Long,
        source: Long,
        url: String,
        artist: String?,
        author: String?,
        description: String?,
        genre: List<String>?,
        title: String,
        status: Long,
        thumbnailUrl: String?,
        backgroundUrl: String?,
        favorite: Boolean,
        lastUpdate: Long?,
        nextUpdate: Long?,
        initialized: Boolean,
        viewerFlags: Long,
        chapterFlags: Long,
        coverLastModified: Long,
        backgroundLastModified: Long,
        dateAdded: Long,
        updateStrategy: AnimeUpdateStrategy,
        calculateInterval: Long,
        lastModifiedAt: Long,
        favoriteModifiedAt: Long?,
        version: Long,
        @Suppress("UNUSED_PARAMETER")
        isSyncing: Long,
    ): Anime = Anime(
        id = id,
        source = source,
        favorite = favorite,
        lastUpdate = lastUpdate ?: 0,
        nextUpdate = nextUpdate ?: 0,
        fetchInterval = calculateInterval.toInt(),
        dateAdded = dateAdded,
        viewerFlags = viewerFlags,
        episodeFlags = chapterFlags,
        coverLastModified = coverLastModified,
        backgroundLastModified = backgroundLastModified,
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genre = genre,
        status = status,
        thumbnailUrl = thumbnailUrl,
        backgroundUrl = backgroundUrl,
        updateStrategy = updateStrategy,
        initialized = initialized,
        lastModifiedAt = lastModifiedAt,
        favoriteModifiedAt = favoriteModifiedAt,
        version = version,
    )

    fun mapLibraryAnime(
        id: Long,
        source: Long,
        url: String,
        artist: String?,
        author: String?,
        description: String?,
        genre: List<String>?,
        title: String,
        status: Long,
        thumbnailUrl: String?,
        backgroundUrl: String?,
        favorite: Boolean,
        lastUpdate: Long?,
        nextUpdate: Long?,
        initialized: Boolean,
        viewerFlags: Long,
        chapterFlags: Long,
        coverLastModified: Long,
        backgroundLastModified: Long,
        dateAdded: Long,
        updateStrategy: AnimeUpdateStrategy,
        calculateInterval: Long,
        lastModifiedAt: Long,
        favoriteModifiedAt: Long?,
        version: Long,
        isSyncing: Long,
        totalCount: Long,
        seenCount: Double,
        latestUpload: Long,
        episodeFetchedAt: Long,
        lastSeen: Long,
        bookmarkCount: Double,
        category: Long,
    ): LibraryAnime = LibraryAnime(
        anime = mapAnime(
            id,
            source,
            url,
            artist,
            author,
            description,
            genre,
            title,
            status,
            thumbnailUrl,
            backgroundUrl,
            favorite,
            lastUpdate,
            nextUpdate,
            initialized,
            viewerFlags,
            chapterFlags,
            coverLastModified,
            backgroundLastModified,
            dateAdded,
            updateStrategy,
            calculateInterval,
            lastModifiedAt,
            favoriteModifiedAt,
            version,
            isSyncing,
        ),
        category = category,
        totalEpisodes = totalCount,
        seenCount = seenCount.toLong(),
        bookmarkCount = bookmarkCount.toLong(),
        latestUpload = latestUpload,
        episodeFetchedAt = episodeFetchedAt,
        lastSeen = lastSeen,
    )
}
