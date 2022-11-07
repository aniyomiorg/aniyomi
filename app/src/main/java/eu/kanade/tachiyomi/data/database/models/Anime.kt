package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.domain.anime.model.Anime as DomainAnime

interface Anime : SAnime {

    var id: Long?

    var source: Long

    var favorite: Boolean

    // last time the episode list changed in any way
    var last_update: Long

    var date_added: Long

    var viewer_flags: Int

    var episode_flags: Int

    var cover_last_modified: Long

    fun sortDescending(): Boolean {
        return episode_flags and DomainAnime.EPISODE_SORT_DIR_MASK.toInt() == DomainAnime.EPISODE_SORT_DESC.toInt()
    }

    private fun setEpisodeFlags(flag: Int, mask: Int) {
        episode_flags = episode_flags and mask.inv() or (flag and mask)
    }

    private fun setViewerFlags(flag: Int, mask: Int) {
        viewer_flags = viewer_flags and mask.inv() or (flag and mask)
    }

    // Used to display the episode's title one way or another
    var displayMode: Int
        get() = episode_flags and DomainAnime.EPISODE_DISPLAY_MASK.toInt()
        set(mode) = setEpisodeFlags(mode, DomainAnime.EPISODE_DISPLAY_MASK.toInt())

    var seenFilter: Int
        get() = episode_flags and DomainAnime.EPISODE_UNSEEN_MASK.toInt()
        set(filter) = setEpisodeFlags(filter, DomainAnime.EPISODE_UNSEEN_MASK.toInt())

    var downloadedFilter: Int
        get() = episode_flags and DomainAnime.EPISODE_DOWNLOADED_MASK.toInt()
        set(filter) = setEpisodeFlags(filter, DomainAnime.EPISODE_DOWNLOADED_MASK.toInt())

    var bookmarkedFilter: Int
        get() = episode_flags and DomainAnime.EPISODE_BOOKMARKED_MASK.toInt()
        set(filter) = setEpisodeFlags(filter, DomainAnime.EPISODE_BOOKMARKED_MASK.toInt())

    var sorting: Int
        get() = episode_flags and DomainAnime.EPISODE_SORTING_MASK.toInt()
        set(sort) = setEpisodeFlags(sort, DomainAnime.EPISODE_SORTING_MASK.toInt())

    var skipIntroLength: Int
        get() = viewer_flags and 0x000000FF
        set(skipIntro) = setViewerFlags(skipIntro, 0x000000FF)

    companion object {
        fun create(pathUrl: String, title: String, source: Long = 0): Anime = AnimeImpl().apply {
            url = pathUrl
            this.title = title
            this.source = source
        }
    }
}

fun Anime.toDomainAnime(): DomainAnime? {
    val mangaId = id ?: return null
    return DomainAnime(
        id = mangaId,
        source = source,
        favorite = favorite,
        lastUpdate = last_update,
        dateAdded = date_added,
        viewerFlags = viewer_flags.toLong(),
        episodeFlags = episode_flags.toLong(),
        coverLastModified = cover_last_modified,
        url = url,
        title = title,
        artist = artist,
        author = author,
        description = description,
        genre = getGenres(),
        status = status.toLong(),
        thumbnailUrl = thumbnail_url,
        initialized = initialized,
        updateStrategy = update_strategy,
    )
}
