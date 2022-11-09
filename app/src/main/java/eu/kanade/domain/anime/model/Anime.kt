package eu.kanade.domain.anime.model

import eu.kanade.data.listOfStringsAdapter
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.database.models.AnimeImpl
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.Serializable
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
    val updateStrategy: UpdateStrategy,
    val initialized: Boolean,
) : Serializable {

    val sorting: Long
        get() = episodeFlags and EPISODE_SORTING_MASK

    val displayMode: Long
        get() = episodeFlags and EPISODE_DISPLAY_MASK

    val unseenFilterRaw: Long
        get() = episodeFlags and EPISODE_UNSEEN_MASK

    val downloadedFilterRaw: Long
        get() = episodeFlags and EPISODE_DOWNLOADED_MASK

    val bookmarkedFilterRaw: Long
        get() = episodeFlags and EPISODE_BOOKMARKED_MASK

    val unseenFilter: TriStateFilter
        get() = when (unseenFilterRaw) {
            EPISODE_SHOW_UNSEEN -> TriStateFilter.ENABLED_IS
            EPISODE_SHOW_SEEN -> TriStateFilter.ENABLED_NOT
            else -> TriStateFilter.DISABLED
        }

    val downloadedFilter: TriStateFilter
        get() {
            if (forceDownloaded()) return TriStateFilter.ENABLED_IS
            return when (downloadedFilterRaw) {
                EPISODE_SHOW_DOWNLOADED -> TriStateFilter.ENABLED_IS
                EPISODE_SHOW_NOT_DOWNLOADED -> TriStateFilter.ENABLED_NOT
                else -> TriStateFilter.DISABLED
            }
        }

    val bookmarkedFilter: TriStateFilter
        get() = when (bookmarkedFilterRaw) {
            EPISODE_SHOW_BOOKMARKED -> TriStateFilter.ENABLED_IS
            EPISODE_SHOW_NOT_BOOKMARKED -> TriStateFilter.ENABLED_NOT
            else -> TriStateFilter.DISABLED
        }

    fun episodesFiltered(): Boolean {
        return unseenFilter != TriStateFilter.DISABLED ||
            downloadedFilter != TriStateFilter.DISABLED ||
            bookmarkedFilter != TriStateFilter.DISABLED
    }

    fun forceDownloaded(): Boolean {
        return favorite && Injekt.get<BasePreferences>().downloadedOnly().get()
    }

    fun sortDescending(): Boolean {
        return episodeFlags and EPISODE_SORT_DIR_MASK == EPISODE_SORT_DESC
    }

    fun toSAnime(): SAnime = SAnime.create().also {
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

    fun copyFrom(other: SAnime): Anime {
        val author = other.author ?: author
        val artist = other.artist ?: artist
        val description = other.description ?: description
        val genres = if (other.genre != null) {
            other.getGenres()
        } else {
            genre
        }
        val thumbnailUrl = other.thumbnail_url ?: thumbnailUrl
        return this.copy(
            author = author,
            artist = artist,
            description = description,
            genre = genres,
            thumbnailUrl = thumbnailUrl,
            status = other.status.toLong(),
            updateStrategy = other.update_strategy,
            initialized = other.initialized && initialized,
        )
    }

    companion object {
        // Generic filter that does not filter anything
        const val SHOW_ALL = 0x00000000L

        const val EPISODE_SORT_DESC = 0x00000000L
        const val EPISODE_SORT_ASC = 0x00000001L
        const val EPISODE_SORT_DIR_MASK = 0x00000001L

        const val EPISODE_SHOW_UNSEEN = 0x00000002L
        const val EPISODE_SHOW_SEEN = 0x00000004L
        const val EPISODE_UNSEEN_MASK = 0x00000006L

        const val EPISODE_SHOW_DOWNLOADED = 0x00000008L
        const val EPISODE_SHOW_NOT_DOWNLOADED = 0x00000010L
        const val EPISODE_DOWNLOADED_MASK = 0x00000018L

        const val EPISODE_SHOW_BOOKMARKED = 0x00000020L
        const val EPISODE_SHOW_NOT_BOOKMARKED = 0x00000040L
        const val EPISODE_BOOKMARKED_MASK = 0x00000060L

        const val EPISODE_SORTING_SOURCE = 0x00000000L
        const val EPISODE_SORTING_NUMBER = 0x00000100L
        const val EPISODE_SORTING_UPLOAD_DATE = 0x00000200L
        const val EPISODE_SORTING_MASK = 0x00000300L

        const val EPISODE_DISPLAY_NAME = 0x00000000L
        const val EPISODE_DISPLAY_NUMBER = 0x00100000L
        const val EPISODE_DISPLAY_MASK = 0x00100000L

        fun create() = Anime(
            id = -1L,
            url = "",
            title = "",
            source = -1L,
            favorite = false,
            lastUpdate = 0L,
            dateAdded = 0L,
            viewerFlags = 0L,
            episodeFlags = 0L,
            coverLastModified = 0L,
            artist = null,
            author = null,
            description = null,
            genre = null,
            status = 0L,
            thumbnailUrl = null,
            updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
            initialized = false,
        )
    }
}

enum class TriStateFilter {
    DISABLED, // Disable filter
    ENABLED_IS, // Enabled with "is" filter
    ENABLED_NOT, // Enabled with "not" filter
}

fun TriStateFilter.toTriStateGroupState(): ExtendedNavigationView.Item.TriStateGroup.State {
    return when (this) {
        TriStateFilter.DISABLED -> ExtendedNavigationView.Item.TriStateGroup.State.IGNORE
        TriStateFilter.ENABLED_IS -> ExtendedNavigationView.Item.TriStateGroup.State.INCLUDE
        TriStateFilter.ENABLED_NOT -> ExtendedNavigationView.Item.TriStateGroup.State.EXCLUDE
    }
}

// TODO: Remove when all deps are migrated
fun Anime.toDbAnime(): DbAnime = AnimeImpl().also {
    it.id = id
    it.source = source
    it.favorite = favorite
    it.last_update = lastUpdate
    it.date_added = dateAdded
    it.viewer_flags = viewerFlags.toInt()
    it.episode_flags = episodeFlags.toInt()
    it.cover_last_modified = coverLastModified
    it.url = url
    it.title = title
    it.artist = artist
    it.author = author
    it.description = description
    it.genre = genre?.let(listOfStringsAdapter::encode)
    it.status = status.toInt()
    it.thumbnail_url = thumbnailUrl
    it.update_strategy = updateStrategy
    it.initialized = initialized
}

fun Anime.toAnimeUpdate(): AnimeUpdate {
    return AnimeUpdate(
        id = id,
        source = source,
        favorite = favorite,
        lastUpdate = lastUpdate,
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
        updateStrategy = update_strategy,
        initialized = initialized,
        source = sourceId,
    )
}

fun Anime.isLocal(): Boolean = source == LocalSource.ID

fun Anime.hasCustomCover(coverCache: AnimeCoverCache = Injekt.get()): Boolean {
    return coverCache.getCustomCoverFile(id).exists()
}
