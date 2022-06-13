package eu.kanade.domain.anime.model

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.source.LocalSource
import tachiyomi.animesource.model.AnimeInfo
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.data.database.models.Anime as DbAnime

data class Anime(
    val id: Long,
    val source: Long,
    val favorite: Boolean,
    val lastUpdate: Long,
    val dateAdded: Long,
    val viewerFlags: Long,
    val episodeFlags: Long,
    val coverLastModified: Long,
    val url: String,
    val title: String,
    val artist: String?,
    val author: String?,
    val description: String?,
    val genre: List<String>?,
    val status: Long,
    val thumbnailUrl: String?,
    val initialized: Boolean,
) {

    val sorting: Long
        get() = episodeFlags and EPISODE_SORTING_MASK

    fun toSAnime(): SAnime {
        return SAnime.create().also {
            it.url = url
            it.title = title
            it.artist = artist
            it.author = author
            it.description = description
            it.genre = genre.orEmpty().joinToString()
            it.status = status.toInt()
            it.thumbnail_url = thumbnailUrl
            it.initialized = initialized
        }
    }

    companion object {

        // Generic filter that does not filter anything
        const val SHOW_ALL = 0x00000000L

        const val EPISODE_SORTING_SOURCE = 0x00000000L
        const val EPISODE_SORTING_NUMBER = 0x00000100L
        const val EPISODE_SORTING_UPLOAD_DATE = 0x00000200L
        const val EPISODE_SORTING_MASK = 0x00000300L
    }
}

// TODO: Remove when all deps are migrated
fun Anime.toDbAnime(): DbAnime = DbAnime.create(url, title, source).also {
    it.id = id
    it.favorite = favorite
    it.last_update = lastUpdate
    it.date_added = dateAdded
    it.viewer_flags = viewerFlags.toInt()
    it.episode_flags = episodeFlags.toInt()
    it.cover_last_modified = coverLastModified
}

fun Anime.toAnimeInfo(): AnimeInfo = AnimeInfo(
    artist = artist ?: "",
    author = author ?: "",
    cover = thumbnailUrl ?: "",
    description = description ?: "",
    genres = genre ?: emptyList(),
    key = url,
    status = status.toInt(),
    title = title,
)

fun Anime.isLocal(): Boolean = source == LocalSource.ID

fun Anime.hasCustomCover(coverCache: AnimeCoverCache = Injekt.get()): Boolean {
    return coverCache.getCustomCoverFile(id).exists()
}
