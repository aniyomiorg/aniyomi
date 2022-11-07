package eu.kanade.data.anime

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.animelib.model.AnimelibAnime
import eu.kanade.tachiyomi.source.model.UpdateStrategy

val animeMapper: (Long, Long, String, String?, String?, String?, List<String>?, String, Long, String?, Boolean, Long?, Long?, Boolean, Long, Long, Long, Long, UpdateStrategy) -> Anime =
    { id, source, url, artist, author, description, genre, title, status, thumbnailUrl, favorite, lastUpdate, _, initialized, viewerFlags, episodeFlags, coverLastModified, dateAdded, updateStrategy ->
        Anime(
            id = id,
            source = source,
            favorite = favorite,
            lastUpdate = lastUpdate ?: 0,
            dateAdded = dateAdded,
            viewerFlags = viewerFlags,
            episodeFlags = episodeFlags,
            coverLastModified = coverLastModified,
            url = url,
            title = title,
            artist = artist,
            author = author,
            description = description,
            genre = genre,
            status = status,
            thumbnailUrl = thumbnailUrl,
            updateStrategy = updateStrategy,
            initialized = initialized,
        )
    }
val animelibAnime: (Long, Long, String, String?, String?, String?, List<String>?, String, Long, String?, Boolean, Long?, Long?, Boolean, Long, Long, Long, Long, UpdateStrategy, Long, Long, Long, Long, Long, Long, Long) -> AnimelibAnime =
    { id, source, url, artist, author, description, genre, title, status, thumbnailUrl, favorite, lastUpdate, nextUpdate, initialized, viewerFlags, episodeFlags, coverLastModified, dateAdded, updateStrategy, totalCount, seenCount, latestUpload, episodeFetchedAt, lastSeen, bookmarkCount, category ->
        AnimelibAnime(
            anime = animeMapper(
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
                favorite,
                lastUpdate,
                nextUpdate,
                initialized,
                viewerFlags,
                episodeFlags,
                coverLastModified,
                dateAdded,
                updateStrategy,
            ),
            category = category,
            totalEpisodes = totalCount,
            seenCount = seenCount,
            bookmarkCount = bookmarkCount,
            latestUpload = latestUpload,
            episodeFetchedAt = episodeFetchedAt,
            lastSeen = lastSeen,
        )
    }
