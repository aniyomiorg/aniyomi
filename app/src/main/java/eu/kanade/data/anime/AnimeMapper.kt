package eu.kanade.data.anime

import eu.kanade.domain.anime.model.Anime
import eu.kanade.domain.episode.model.Episode
import eu.kanade.tachiyomi.data.database.models.AnimelibAnime

val animeMapper: (Long, Long, String, String?, String?, String?, List<String>?, String, Long, String?, Boolean, Long?, Long?, Boolean, Long, Long, Long, Long) -> Anime =
    { id, source, url, artist, author, description, genre, title, status, thumbnailUrl, favorite, lastUpdate, _, initialized, viewer, episodeFlags, coverLastModified, dateAdded ->
        Anime(
            id = id,
            source = source,
            favorite = favorite,
            lastUpdate = lastUpdate ?: 0,
            dateAdded = dateAdded,
            viewerFlags = viewer,
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
            initialized = initialized,
        )
    }

val animeEpisodeMapper: (Long, Long, String, String?, String?, String?, List<String>?, String, Long, String?, Boolean, Long?, Long?, Boolean, Long, Long, Long, Long, Long, Long, String, String, String?, Boolean, Boolean, Long, Long, Float, Long, Long, Long) -> Pair<Anime, Episode> =
    { _id, source, url, artist, author, description, genre, title, status, thumbnailUrl, favorite, lastUpdate, next_update, initialized, viewerFlags, episodeFlags, coverLastModified, dateAdded, episodeId, animeId, chapterUrl, name, scanlator, seen, bookmark, lastSecondSeen, totalSeconds, episodeNumber, sourceOrder, dateFetch, dateUpload ->
        Anime(
            id = _id,
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
            initialized = initialized,
        ) to Episode(
            id = episodeId,
            animeId = animeId,
            seen = seen,
            bookmark = bookmark,
            lastSecondSeen = lastSecondSeen,
            totalSeconds = totalSeconds,
            dateFetch = dateFetch,
            sourceOrder = sourceOrder,
            url = chapterUrl,
            name = name,
            dateUpload = dateUpload,
            episodeNumber = episodeNumber,
            scanlator = scanlator,
        )
    }

val animelibAnime: (Long, Long, String, String?, String?, String?, List<String>?, String, Long, String?, Boolean, Long?, Long?, Boolean, Long, Long, Long, Long, Long, Long, Long) -> AnimelibAnime =
    { _id, source, url, artist, author, description, genre, title, status, thumbnail_url, favorite, last_update, next_update, initialized, viewer, episode_flags, cover_last_modified, date_added, unseen_count, seen_count, category ->
        AnimelibAnime().apply {
            this.id = _id
            this.source = source
            this.url = url
            this.artist = artist
            this.author = author
            this.description = description
            this.genre = genre?.joinToString()
            this.title = title
            this.status = status.toInt()
            this.thumbnail_url = thumbnail_url
            this.favorite = favorite
            this.last_update = last_update ?: 0
            this.initialized = initialized
            this.viewer_flags = viewer.toInt()
            this.episode_flags = episode_flags.toInt()
            this.cover_last_modified = cover_last_modified
            this.date_added = date_added
            this.unseenCount = unseen_count.toInt()
            this.seenCount = seen_count.toInt()
            this.category = category.toInt()
        }
    }
