package eu.kanade.domain.entries.anime.model

import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.AnimeBackgroundCache
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entries.anime.model.Anime
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// TODO: move these into the domain model
val Anime.downloadedFilter: TriState
    get() {
        if (Injekt.get<BasePreferences>().downloadedOnly().get()) return TriState.ENABLED_IS
        return when (downloadedFilterRaw) {
            Anime.EPISODE_SHOW_DOWNLOADED -> TriState.ENABLED_IS
            Anime.EPISODE_SHOW_NOT_DOWNLOADED -> TriState.ENABLED_NOT
            else -> TriState.DISABLED
        }
    }

val Anime.seasonDownloadedFilter: TriState
    get() {
        if (Injekt.get<BasePreferences>().downloadedOnly().get()) return TriState.ENABLED_IS
        return when (seasonDownloadedFilterRaw) {
            Anime.SEASON_SHOW_DOWNLOADED -> TriState.ENABLED_IS
            Anime.SEASON_SHOW_NOT_DOWNLOADED -> TriState.ENABLED_NOT
            else -> TriState.DISABLED
        }
    }

fun Anime.episodesFiltered(): Boolean {
    return unseenFilter != TriState.DISABLED ||
        downloadedFilter != TriState.DISABLED ||
        bookmarkedFilter != TriState.DISABLED ||
        fillermarkedFilter != TriState.DISABLED
}

fun Anime.seasonsFiltered(): Boolean {
    return seasonDownloadedFilter != TriState.DISABLED ||
        seasonUnseenFilter != TriState.DISABLED ||
        seasonStartedFilter != TriState.DISABLED ||
        seasonCompletedFilter != TriState.DISABLED ||
        seasonBookmarkedFilter != TriState.DISABLED ||
        seasonFillermarkedFilter != TriState.DISABLED
}

fun Anime.toSAnime(): SAnime = SAnime.create().also {
    it.url = url
    it.title = title
    it.artist = artist
    it.author = author
    it.description = description
    it.genre = genre.orEmpty().joinToString()
    it.status = status.toInt()
    it.thumbnail_url = thumbnailUrl
    it.background_url = backgroundUrl
    it.fetch_type = fetchType
    it.season_number = seasonNumber
    it.initialized = initialized
}

fun Anime.copyFrom(other: SAnime): Anime {
    val author = other.author ?: author
    val artist = other.artist ?: artist
    val description = other.description ?: description
    val genres = if (other.genre != null) {
        other.getGenres()
    } else {
        genre
    }
    val thumbnailUrl = other.thumbnail_url ?: thumbnailUrl
    val backgroundUrl = other.background_url ?: backgroundUrl
    return this.copy(
        author = author,
        artist = artist,
        description = description,
        genre = genres,
        thumbnailUrl = thumbnailUrl,
        backgroundUrl = backgroundUrl,
        status = other.status.toLong(),
        updateStrategy = other.update_strategy,
        fetchType = other.fetch_type,
        seasonNumber = other.season_number,
        initialized = other.initialized && initialized,
    )
}

fun SAnime.toDomainAnime(sourceId: Long): Anime {
    return Anime.create().copy(
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genre = getGenres(),
        status = status.toLong(),
        thumbnailUrl = thumbnail_url,
        backgroundUrl = background_url,
        updateStrategy = update_strategy,
        fetchType = fetch_type,
        seasonNumber = season_number,
        initialized = initialized,
        source = sourceId,
    )
}

fun Anime.hasCustomCover(coverCache: AnimeCoverCache = Injekt.get()): Boolean {
    return coverCache.getCustomCoverFile(id).exists()
}

fun Anime.hasCustomBackground(backgroundCache: AnimeBackgroundCache = Injekt.get()): Boolean {
    return backgroundCache.getCustomBackgroundFile(id).exists()
}
